package com.example.paymentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Records the event keys we've already handled so a redelivery (Kafka is
 * at-least-once) doesn't apply the same status update twice. The producer
 * sends no event id, so the key is derived in PaymentStatusEventService as
 * paymentId+status+eventTimestamp.
 *
 * <p>The unique constraint on event_id is what actually enforces idempotency;
 * the existsByEventId() check is just a fast path for the common case.
 */
@Entity
@Table(name = "processed_events", uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}