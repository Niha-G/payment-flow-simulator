package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.event.PaymentEvent;
import com.example.paymentservice.kafka.PaymentEventPublisher;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal MAX_ALLOWED_AMOUNT = new BigDecimal("100000.00");

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public Payment submitPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setSenderAccount(request.getSenderAccount());
        payment.setReceiverAccount(request.getReceiverAccount());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());

        // Basic validation rules - mirrors real payment validation pipelines
        if (request.getSenderAccount().equals(request.getReceiverAccount())) {
            payment.setStatus(PaymentStatus.REJECTED);
            log.warn("Payment {} rejected: sender and receiver accounts match", payment.getId());
        } else if (request.getAmount().compareTo(MAX_ALLOWED_AMOUNT) > 0) {
            payment.setStatus(PaymentStatus.REJECTED);
            log.warn("Payment {} rejected: amount exceeds limit", payment.getId());
        } else {
            payment.setStatus(PaymentStatus.VALIDATED);
        }

        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentEvent(
                saved.getId(), saved.getSenderAccount(), saved.getReceiverAccount(),
                saved.getAmount(), saved.getCurrency(), saved.getStatus()
        ));

        return saved;
    }

    public Optional<Payment> getPayment(String id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment updateStatus(String id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        payment.setStatus(status);
        Payment updated = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentEvent(
                updated.getId(), updated.getSenderAccount(), updated.getReceiverAccount(),
                updated.getAmount(), updated.getCurrency(), updated.getStatus()
        ));

        return updated;
    }
}
