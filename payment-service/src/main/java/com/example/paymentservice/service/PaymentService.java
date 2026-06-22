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
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validates and persists payments, enriches them with an AI-generated summary,
 * and publishes the resulting status events.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal MAX_ALLOWED_AMOUNT = new BigDecimal("100000.00");

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final WebClient webClient;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher,
                          WebClient.Builder webClientBuilder) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.webClient = webClientBuilder.baseUrl("https://api.anthropic.com").build();
    }

    public Payment submitPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setSenderAccount(request.getSenderAccount());
        payment.setReceiverAccount(request.getReceiverAccount());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());

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

        saved.setAiSummary(fetchAiSummary(saved));
        saved = paymentRepository.save(saved);

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

    /**
     * Asks the Anthropic API for a one-line summary of the payment; returns
     * {@code null} on failure so summary generation never blocks a payment.
     */
    @SuppressWarnings("unchecked")
    private String fetchAiSummary(Payment payment) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        String prompt = String.format(
                "Give a one-sentence summary of this payment: %s sent %s %s to %s (status: %s).",
                payment.getSenderAccount(), payment.getAmount(), payment.getCurrency(),
                payment.getReceiverAccount(), payment.getStatus());

        Map<String, Object> body = Map.of(
                "model", "claude-haiku-4-5-20251001",
                "max_tokens", 100,
                "messages", List.of(Map.of("role", "user", "content", prompt)));

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            return (String) content.get(0).get("text");
        } catch (Exception e) {
            log.warn("Failed to get AI summary for payment {}: {}", payment.getId(), e.getMessage());
            return null;
        }
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
