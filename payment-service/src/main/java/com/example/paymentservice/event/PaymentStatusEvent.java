package com.example.paymentservice.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inbound Kafka event for an async payment status update.
 *
 * Field names/types mirror the producer's PaymentEvent
 * (com.example.paymentservice.event.PaymentEvent) so Jackson deserializes
 * the published JSON directly. `status` is carried as a String (the producer
 * serializes the PaymentStatus enum by name) and converted to the enum in
 * PaymentStatusEventService.
 */
public class PaymentStatusEvent {

    private String paymentId;
    private String senderAccount;
    private String receiverAccount;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant eventTimestamp;

    public PaymentStatusEvent() {
        // required by Jackson for deserialization
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSenderAccount() { return senderAccount; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }

    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    @Override
    public String toString() {
        return "PaymentStatusEvent{paymentId='%s', status='%s', eventTimestamp=%s}"
                .formatted(paymentId, status, eventTimestamp);
    }
}