package com.example.paymentservice.exception;

/**
 * A status event referenced a paymentId we don't have. Non-retryable (see
 * KafkaConsumerConfig) — the record won't appear on retry, so it's dead-lettered
 * for manual investigation.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String paymentId) {
        super("No payment found for paymentId=" + paymentId);
    }
}