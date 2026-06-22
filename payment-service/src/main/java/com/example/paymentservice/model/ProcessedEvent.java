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
 * Tracks derived event keys that have already been processed so that
 * redelivery (at-least-once semantics) doesn't double-apply a status
 * update. The producer emits no unique event id, so the key stored here is
 * derived in PaymentStatusEventService (paymentId+status+eventTimestamp).
 * The unique constraint on event_id is what actually enforces idempotency —
 * the existsByEventId() check is just a fast-path that avoids hitting that
 * constraint on the common case.
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