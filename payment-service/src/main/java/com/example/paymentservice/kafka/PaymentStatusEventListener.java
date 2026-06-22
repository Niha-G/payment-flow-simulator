package com.example.paymentservice.kafka;

import com.example.paymentservice.event.PaymentStatusEvent;
import com.example.paymentservice.service.PaymentStatusEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusEventListener.class);

    private final PaymentStatusEventService service;

    public PaymentStatusEventListener(PaymentStatusEventService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-status-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentStatusEvent(PaymentStatusEvent event, Acknowledgment ack) {
        log.debug("Received {}", event);
        service.process(event);
        ack.acknowledge();
    }
}