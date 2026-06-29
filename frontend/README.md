# Payment Flow Simulator — Frontend

React frontend for the payment flow simulator. Connects to the Spring Boot backend on port 8080.

## Running locally

Make sure the backend is running first, then:

```bash
npm install
npm start
```

Opens at http://localhost:3000.

## Features

- Submit payments with sender/receiver account, amount, and currency
- Real-time AI analysis on each payment (risk score, category, anomaly flag)
- Payment history table with click-to-expand detail view
