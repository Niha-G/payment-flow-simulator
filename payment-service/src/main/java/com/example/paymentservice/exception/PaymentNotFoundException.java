package com.example.paymentservice.exception;

/**
 * Thrown when a status event references a paymentId that doesn't exist.
 * This is treated as a non-retryable condition (see KafkaConsumerConfig) —
 * retrying won't make a missing record appear, so the event goes straight
 * to the dead letter topic for manual investigation.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String paymentId) {
        super("No payment found for paymentId=" + paymentId);
    }
}