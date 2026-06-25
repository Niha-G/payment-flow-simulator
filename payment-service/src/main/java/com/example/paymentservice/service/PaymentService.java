package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.event.PaymentEvent;
import com.example.paymentservice.kafka.PaymentEventPublisher;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Application service for the payment lifecycle: validates and persists incoming
 * payments, enriches them with derived insights, and publishes status events for
 * downstream consumers.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal MAX_ALLOWED_AMOUNT = new BigDecimal("100000.00");

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final WebClient webClient;

    @Value("${app.insights.api-key}")
    private String apiKey;

    @Value("${app.insights.model}")
    private String model;

    @Value("${app.insights.version-header}")
    private String versionHeader;

    @Value("${app.insights.version}")
    private String version;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher,
                          WebClient.Builder webClientBuilder,
                          @Value("${app.insights.base-url}") String baseUrl) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
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

        fetchAndApplyInsights(saved);
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
     * Enriches the given payment in place with derived insights: a short summary,
     * a classification, a risk score, and an anomaly flag. Any failure is logged
     * and suppressed so that enrichment never blocks payment processing; in that
     * case the payment is left without insights.
     *
     * @param payment the persisted payment to enrich
     */
    @SuppressWarnings("unchecked")
    private void fetchAndApplyInsights(Payment payment) {
        String prompt = String.format(
                "Analyze this payment and respond with ONLY a JSON object, no other text:\n" +
                        "Sender: %s, Receiver: %s, Amount: %s %s, Status: %s\n\n" +
                        "Return exactly this structure:\n" +
                        "{\"summary\": \"one sentence description\", " +
                        "\"category\": \"one of: STANDARD, HIGH_VALUE, SUSPICIOUS, INTERNAL\", " +
                        "\"riskScore\": <integer 1-100>, " +
                        "\"anomalyFlag\": <true or false>}",
                payment.getSenderAccount(), payment.getReceiverAccount(),
                payment.getAmount(), payment.getCurrency(), payment.getStatus());

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 200,
                "messages", List.of(Map.of("role", "user", "content", prompt)));

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header(versionHeader, version)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> content =
                    response != null ? (List<Map<String, Object>>) response.get("content") : null;
            if (content == null || content.isEmpty()) {
                log.warn("Insights response missing 'content' for payment {}", payment.getId());
                return;
            }

            String json = (String) content.get(0).get("text");
            json = json.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();
            log.info("Raw insights response: {}", json);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> insights = mapper.readValue(json, Map.class);

            payment.setAiSummary((String) insights.get("summary"));
            payment.setAiCategory((String) insights.get("category"));
            payment.setAiRiskScore((Integer) insights.get("riskScore"));
            payment.setAiAnomalyFlag((Boolean) insights.get("anomalyFlag"));

        } catch (Exception e) {
            log.warn("Failed to get insights for payment {}: {}", payment.getId(), e.getMessage());
            log.warn("Full exception: ", e);
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
