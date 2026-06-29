# Payment Flow Simulator

A portfolio project simulating real-time payment processing — Spring Boot microservice with Kafka event streaming, AI-powered transaction analysis, React frontend, and Docker/CI setup.

> Built for learning/portfolio purposes only. Not affiliated with or based on any employer's code or systems.

## What it does

- Accepts payment submissions via REST API (sender, receiver, amount, currency)
- Validates business rules (sender ≠ receiver, amount limits) and rejects invalid payments
- Enriches each payment with AI-generated analysis: summary, category, risk score, and anomaly flag
- Publishes payment events to Kafka on creation and status change
- Consumes status events with idempotent deduplication and exponential backoff retry
- React frontend for submitting payments and viewing AI insights in real time

## Tech Stack

- Java 21, Spring Boot 3.3 (Web, Data JPA, Validation, Kafka, WebFlux)
- H2 in-memory database
- Apache Kafka (event publishing + consumption)
- Claude API (AI transaction analysis)
- React + Axios (frontend)
- Docker + docker-compose
- GitHub Actions CI
- Gradle

## Project Structure
payment-flow-simulator/

├── payment-service/          Spring Boot backend

│   └── src/main/java/com/example/paymentservice/

│       ├── controller/       REST endpoints

│       ├── service/          Business logic + AI insights

│       ├── repository/       Data access

│       ├── model/            Entities + enums

│       ├── dto/              Request/response objects

│       ├── event/            Kafka event payloads

│       ├── kafka/            Producer + consumer

│       └── config/           Kafka configuration

└── frontend/                 React frontend
## Running locally

### Prerequisites
- Java 21+
- Docker (for Kafka)
- Node 18+ (for frontend)
- Anthropic API key

### Start Kafka
```bash
docker-compose up -d
```

### Run the backend
```bash
cd payment-service
export ANTHROPIC_API_KEY=your_key_here
./gradlew bootRun
```

### Run the frontend
```bash
cd frontend
npm install
npm start
```

Frontend at `http://localhost:3000`, backend at `http://localhost:8080`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments` | Submit a new payment |
| GET | `/api/payments` | List all payments |
| GET | `/api/payments/{id}` | Get payment by ID |
| PATCH | `/api/payments/{id}/status?status=COMPLETED` | Update payment status |

### Example — submit a payment
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

### Example response (with AI insights)
```json
{
  "id": "97f42f49-063d-4e4d-8e51-617edf5efa48",
  "senderAccount": "ACC-1001",
  "receiverAccount": "ACC-2002",
  "amount": 250.00,
  "currency": "USD",
  "status": "VALIDATED",
  "aiSummary": "Standard domestic transfer of $250.00 from ACC-1001 to ACC-2002.",
  "aiCategory": "STANDARD",
  "aiRiskScore": 5,
  "aiAnomalyFlag": false
}
```

## What's built

- [x] REST API with validation and rejection logic
- [x] Kafka event publishing (non-blocking, bounded timeouts)
- [x] Kafka consumer with idempotent dedup and exponential backoff retry to DLT
- [x] AI transaction analysis via Claude API
- [x] React frontend with real-time AI insights display
- [x] Docker + docker-compose
- [x] GitHub Actions CI (build + test + Docker image)