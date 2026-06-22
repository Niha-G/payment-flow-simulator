package com.example.paymentservice.service;

import com.example.paymentservice.event.PaymentStatusEvent;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.model.ProcessedEvent;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Verifies the retry + dead-letter path: a mocked repository fails the persist
 * step on every attempt, so the consumer exhausts its backoff retries and the
 * record is republished to "{topic}.DLT". Runs against an embedded broker.
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"payment-events-topic", "payment-events-topic.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class PaymentStatusEventServiceDltIntegrationTest {

    private static final String TOPIC = "payment-events-topic";
    private static final String DLT_TOPIC = "payment-events-topic.DLT";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private ProcessedEventRepository processedEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaTemplate<String, Object> kafkaTemplate;
    private Consumer<String, String> dltConsumer;

    @BeforeEach
    void setUp() {
        // Nothing is "already processed" — force the flow to reach the save() call.
        given(processedEventRepository.existsByEventId(anyString())).willReturn(false);
        // The transient persist failure: every save() blows up, so no retry can succeed.
        given(processedEventRepository.save(any(ProcessedEvent.class)))
                .willThrow(new RuntimeException("simulated transient persist failure"));

        // Producer used to publish the inbound event straight onto the topic.
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        // producerProps() defaults the key serializer to IntegerSerializer; our keys are Strings.
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);

        // Plain-string consumer on the DLT so we can inspect the republished payload as JSON.
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("dlt-verifier-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        dltConsumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        dltConsumer.subscribe(Collections.singletonList(DLT_TOPIC));

        // Make sure the application's listener has been assigned its partition
        // before we publish, so the record can't be missed.
        for (MessageListenerContainer container : endpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    @AfterEach
    void tearDown() {
        if (dltConsumer != null) {
            dltConsumer.close();
        }
    }

    @Test
    void exhaustedRetriesLandOnDltWithSamePaymentId() {
        String paymentId = "pay-dlt-1";
        seedPayment(paymentId);

        PaymentStatusEvent event = new PaymentStatusEvent();
        event.setPaymentId(paymentId);
        event.setSenderAccount("ACC-SENDER");
        event.setReceiverAccount("ACC-RECEIVER");
        event.setAmount(new BigDecimal("100.00"));
        event.setCurrency("USD");
        event.setStatus(PaymentStatus.COMPLETED.name());
        event.setEventTimestamp(Instant.parse("2026-06-22T10:15:30.00Z"));

        kafkaTemplate.send(TOPIC, paymentId, event);
        kafkaTemplate.flush();

        // Async consumer + ~30s backoff retries, so poll until the record lands on the DLT.
        List<ConsumerRecord<String, String>> dltRecords = new ArrayList<>();
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    dltConsumer.poll(Duration.ofMillis(500))
                            .records(DLT_TOPIC)
                            .forEach(dltRecords::add);
                    assertThat(dltRecords).isNotEmpty();
                });

        ConsumerRecord<String, String> dltRecord = dltRecords.get(0);
        JsonNode payload = readJson(dltRecord.value());
        assertThat(payload.get("paymentId").asText()).isEqualTo(paymentId);
    }

    private void seedPayment(String paymentId) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setSenderAccount("ACC-SENDER");
        payment.setReceiverAccount("ACC-RECEIVER");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse DLT payload: " + value, e);
        }
    }
}