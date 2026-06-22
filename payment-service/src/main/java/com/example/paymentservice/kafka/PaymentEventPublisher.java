package com.example.paymentservice.kafka;

import com.example.paymentservice.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes {@link PaymentEvent}s to Kafka on a fire-and-forget basis.
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${app.kafka.topic.payment-events}")
    private String topic;

    public PaymentEventPublisher(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes the event asynchronously, logging and swallowing any failure so
     * that a Kafka outage never blocks the calling payment flow.
     */
    public void publish(PaymentEvent event) {
        log.info("Publishing payment event for paymentId={} status={}", event.getPaymentId(), event.getStatus());
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(topic, event.getPaymentId(), event);
            } catch (Exception e) {
                log.warn("Failed to publish payment event for paymentId={}: {}",
                        event.getPaymentId(), e.getMessage());
            }
        });
    }
}
