# Payment Flow Simulator

A personal portfolio project simulating a simplified real-time payment processing flow — built to demonstrate Spring Boot microservices, event-driven architecture with Kafka, and (in later phases) containerization and CI/CD.

> This project is for learning/portfolio purposes only. It is not affiliated with or based on any employer's code, architecture, or proprietary systems.

## What it does

- Accepts payment submissions via REST API (sender, receiver, amount, currency)
- Validates basic business rules (e.g. sender ≠ receiver, amount limits)
- Persists payment records (H2 in-memory DB for now)
- Publishes a `PaymentEvent` to Kafka whenever a payment is created or its status changes
- Exposes endpoints to fetch payment status

## Tech Stack

- Java 17
- Spring Boot 3.3 (Web, Data JPA, Validation, Kafka)
- H2 in-memory database
- Apache Kafka (event publishing)
- Maven

## Project Structure

```
payment-service/
├── src/main/java/com/example/paymentservice/
│   ├── controller/      REST endpoints
│   ├── service/         Business logic
│   ├── repository/      Data access
│   ├── model/           Entities + enums
│   ├── dto/             Request/response objects
│   ├── event/           Kafka event payloads
│   ├── kafka/           Kafka producer
│   └── config/          Kafka configuration
└── src/main/resources/
    └── application.yml
```

## Running locally

### Prerequisites
- Java 17+
- Maven
- Kafka running locally (see below) — or comment out Kafka calls to run without it

### Start Kafka (via Docker)
```bash
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
```

### Run the service
```bash
cd payment-service
mvn spring-boot:run
```

Service runs on `http://localhost:8080`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments` | Submit a new payment |
| GET | `/api/payments` | List all payments |
| GET | `/api/payments/{id}` | Get payment by ID |
| PATCH | `/api/payments/{id}/status?status=COMPLETED` | Update payment status |
| GET | `/api/payments/health` | Health check |

### Example request
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "senderAccount": "ACC-1001",
    "receiverAccount": "ACC-2002",
    "amount": 250.00,
    "currency": "USD"
  }'
```

## Roadmap

- [x] Phase 1: Core payment submission API with validation
- [x] Phase 2: Kafka event publishing on payment creation/status change
- [ ] Phase 3: Consumer service to process events and update status asynchronously
- [ ] Phase 4: React frontend for submitting and tracking payments
- [ ] Phase 5: Dockerize all services with docker-compose
- [ ] Phase 6: GitHub Actions CI/CD pipeline

## Why this project

Built to apply real-time payment processing concepts (validation pipelines, event-driven status updates) in a small, self-contained system using the same core technologies (Spring Boot, Kafka) common in enterprise payment platforms.


## Development Notes

This project was designed and built by me, with Claude Code used as a pair-programming assistant for debugging environment setup issues (Kafka connection handling, Gradle migration) — similar to how GitHub Copilot and Devin AI are used in my current role at Wells Fargo.
