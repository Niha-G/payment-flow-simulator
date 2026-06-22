package com.example.paymentservice.event;

import com.example.paymentservice.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentEvent {

    private String paymentId;
    private String senderAccount;
    private String receiverAccount;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private Instant eventTimestamp;

    public PaymentEvent() {}

    public PaymentEvent(String paymentId, String senderAccount, String receiverAccount,
                         BigDecimal amount, String currency, PaymentStatus status) {
        this.paymentId = paymentId;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.eventTimestamp = Instant.now();
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

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
