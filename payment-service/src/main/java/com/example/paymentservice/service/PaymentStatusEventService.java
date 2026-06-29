package com.example.paymentservice.service;

import com.example.paymentservice.event.PaymentStatusEvent;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.model.ProcessedEvent;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Applies inbound status events to payments, dropping redeliveries via a
 * derived dedupe key so the update only happens once.
 */
@Service
public class PaymentStatusEventService {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusEventService.class);

    private final ProcessedEventRepository processedEventRepository;
    private final PaymentRepository paymentRepository;

    public PaymentStatusEventService(ProcessedEventRepository processedEventRepository,
                                     PaymentRepository paymentRepository) {
        this.processedEventRepository = processedEventRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void process(PaymentStatusEvent event) {
        // No event id from the producer, so build a dedupe key from what we do
        // get. A redelivery repeats paymentId+status+eventTimestamp exactly; a
        // real next transition differs in status (and timestamp).
        String dedupeKey = event.getPaymentId() + ":" + event.getStatus()
                + ":" + event.getEventTimestamp();

        if (processedEventRepository.existsByEventId(dedupeKey)) {
            log.info("Skipping already-processed event {}", dedupeKey);
            return;
        }

        Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(event.getPaymentId()));

        payment.setStatus(PaymentStatus.valueOf(event.getStatus()));
        paymentRepository.save(payment);

        log.info("Applied status update for paymentId={} -> status={}",
                event.getPaymentId(), event.getStatus());

        processedEventRepository.save(new ProcessedEvent(dedupeKey, Instant.now()));
    }
}