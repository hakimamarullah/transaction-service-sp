# Transactions Service

A microservice for handling financial transactions with real-time processing capabilities using Kafka streaming and exchange rate integration.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [API Endpoints](#api-endpoints)
- [Data Model](#data-model)
- [System Requirements](#system-requirements)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Monitoring](#monitoring)

## Overview

The Transactions Service is a RESTful microservice that provides:

- **Transaction Processing**: Accept and process financial transactions with JWT authentication
- **Real-time Streaming**: Kafka integration for event-driven architecture
- **Currency Conversion**: Automatic currency conversion with current exchange rates
- **Transaction History**: Paginated query with monthly filtering and summary statistics
- **IBAN Support**: Full IBAN validation and processing

## Architecture

### System Context Diagram (C4 Level 1)

```
┌─────────────────────────────────────────────────────────────────┐
│                        System Context                           │
│                                                                 │
│  ┌─────────────┐    ┌─────────────────┐    ┌─────────────────┐ │
│  │   Client    │───▶│  Transactions   │───▶│  Exchange Rate  │ │
│  │ Application │    │    Service      │    │    Provider     │ │
│  │             │◀───│                 │    │                 │ │
│  └─────────────┘    └─────────────────┘    └─────────────────┘ │
│                             │                                  │
│                             ▼                                  │
│                      ┌─────────────┐                          │
│                      │    Kafka    │                          │
│                      │   Cluster   │                          │
│                      └─────────────┘                          │
└─────────────────────────────────────────────────────────────────┘
```

### Container Diagram (C4 Level 2)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Transactions Service                         │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │   REST API      │    │   Kafka Stream  │                   │
│  │   Controller    │───▶│   Processor     │                   │
│  │ /api/v1/trans.. │    │                 │                   │
│  └─────────────────┘    └─────────────────┘                   │
│           │                       │                            │
│           ▼                       ▼                            │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │   Transaction   │    │     Kafka       │                   │
│  │    Service      │───▶│   Producer      │                   │
│  │                 │    │                 │                   │
│  └─────────────────┘    └─────────────────┘                   │
│           │                                                    │
│           ▼                                                    │
│  ┌─────────────────┐                                          │
│  │   Exchange      │                                          │
│  │   Rate Client   │                                          │
│  │                 │                                          │
│  └─────────────────┘                                          │
└─────────────────────────────────────────────────────────────────┘
```

### Component Diagram (C4 Level 3)

```
┌─────────────────────────────────────────────────────────────────┐
│                 Transaction Service Components                  │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │                    Web Layer                                │ │
│ │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │ │
│ │  │Transaction  │  │   Error     │  │    Validation       │ │ │
│ │  │ Controller  │  │  Handler    │  │   Middleware        │ │ │
│ │  └─────────────┘  └─────────────┘  └─────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │                  Service Layer                              │ │
│ │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │ │
│ │  │Transaction  │  │  Exchange   │  │    Event            │ │ │
│ │  │  Service    │  │Rate Service │  │  Publisher          │ │ │
│ │  └─────────────┘  └─────────────┘  └─────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │               Infrastructure Layer                          │ │
│ │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │ │
│ │  │   Kafka     │  │  External   │  │    Configuration    │ │ │
│ │  │ Integration │  │ API Client  │  │      Manager        │ │ │
│ │  └─────────────┘  └─────────────┘  └─────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Base URL
```
http://localhost:8080/api/v1/transactions
```

### Authentication
All endpoints require JWT Bearer token authentication:
```
Authorization: Bearer <jwt_token>
```

### Endpoints

#### 1. Get Transactions
```http
GET /api/v1/transactions
```

**Description**: Retrieves paginated transactions with currency conversion and summary information

**Query Parameters**:
- `year` (required): Year to filter transactions (integer)
- `month` (required): Month to filter transactions (1-12)
- `page` (optional): Page number, 0-based (default: 0)
- `size` (optional): Page size (default: 20)
- `baseCurrency` (optional): Base currency for conversion (default: "IDR")

**Response**:
```json
{
  "transactions": [
    {
      "id": "89d3o179-abcd-465b-o9ee-e2d5f6ofEld46",
      "originalAmount": 100.50,
      "originalCurrency": "GBP",
      "convertedAmount": 1805500.75,
      "baseCurrency": "IDR",
      "exchangeRate": 17956.45,
      "accountIban": "CH93-0000-0000-0000-0000-0",
      "valueDate": "2024-08-19",
      "description": "Online payment CHF",
      "type": "DEBIT"
    }
  ],
  "pageInfo": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  },
  "summary": {
    "totalCredits": 5000000.00,
    "totalDebits": 3500000.00,
    "netAmount": 1500000.00,
    "baseCurrency": "IDR",
    "transactionCount": 20
  }
}
```

#### 2. Create Transaction
```http
POST /api/v1/transactions
```

**Description**: Creates a new transaction and publishes to Kafka topic

**Request Body**:
```json
{
  "id": "89d3o179-abcd-465b-o9ee-e2d5f6ofEld46",
  "amount": 100.50,
  "currency": "GBP",
  "accountIban": "CH93-0000-0000-0000-0000-0",
  "valueDate": "2024-08-19",
  "description": "Online payment CHF",
  "customerId": "P-0123456789",
  "type": "DEBIT"
}
```

**Response**:
```
HTTP 200 OK
"Transaction created successfully"
```

### Request/Response Models

#### Transaction (Input Model)
```json
{
  "id": "string (required, min=1)",
  "amount": "number (required)",
  "currency": "string (required, 3 uppercase letters, e.g., 'GBP')",
  "accountIban": "string (required, min=1)",
  "valueDate": "string (required, date format YYYY-MM-DD)",
  "description": "string (required, min=1)",
  "customerId": "string (required, min=1)",
  "type": "enum (required, values: 'CREDIT' | 'DEBIT')"
}
```

#### TransactionDTO (Output Model)
```json
{
  "id": "string",
  "originalAmount": "number",
  "originalCurrency": "string",
  "convertedAmount": "number",
  "baseCurrency": "string",
  "exchangeRate": "number",
  "accountIban": "string",
  "valueDate": "string (date)",
  "description": "string",
  "type": "enum ('CREDIT' | 'DEBIT')"
}
```

## Data Model

### Transaction Entity (Input)

```json
{
  "id": "string",                    // Unique transaction identifier (required)
  "amount": "number",                // Transaction amount (required)
  "currency": "string",              // ISO 4217 currency code, 3 uppercase letters (required)
  "accountIban": "string",           // Account IBAN (required)
  "valueDate": "string",             // Transaction value date in YYYY-MM-DD format (required)
  "description": "string",           // Transaction description (required)
  "customerId": "string",            // Customer identifier (required)
  "type": "enum"                     // Transaction type: "CREDIT" or "DEBIT" (required)
}
```

### TransactionDTO (Output with Exchange Rate)

```json
{
  "id": "string",                    // Unique transaction identifier
  "originalAmount": "number",        // Original transaction amount
  "originalCurrency": "string",      // Original currency
  "convertedAmount": "number",       // Amount converted to base currency
  "baseCurrency": "string",          // Base currency for conversion
  "exchangeRate": "number",          // Exchange rate used for conversion
  "accountIban": "string",           // Account IBAN
  "valueDate": "string",             // Transaction value date
  "description": "string",           // Transaction description
  "type": "enum"                     // Transaction type: "CREDIT" or "DEBIT"
}
```

### Pagination and Summary Models

#### PageInfo
```json
{
  "page": "integer",                 // Current page number (0-based)
  "size": "integer",                 // Page size
  "totalElements": "integer",        // Total number of elements
  "totalPages": "integer",           // Total number of pages
  "first": "boolean",                // Whether this is the first page
  "last": "boolean",                 // Whether this is the last page
  "hasNext": "boolean",              // Whether there are more elements
  "hasPrevious": "boolean"           // Whether there are previous elements
}
```

#### PageSummary
```json
{
  "totalCredits": "number",          // Total credit amount in base currency for current page
  "totalDebits": "number",           // Total debit amount in base currency for current page
  "netAmount": "number",             // Net amount (credits - debits) for current page
  "baseCurrency": "string",          // Base currency used for calculations
  "transactionCount": "integer"      // Number of transactions in current page
}
```

### Kafka Topics

#### Input Topic: `transaction-requests`
- **Purpose**: Receives new transaction requests
- **Partitions**: 1
- **Replication Factor**: 1
- **Key**: account_id
- **Value**: Transaction JSON

#### Output Topic: `transaction-events`
- **Purpose**: Publishes processed transaction events
- **Partitions**: 1
- **Replication Factor**: 1
- **Key**: transaction_id
- **Value**: TransactionEvent JSON

### Data Flow

```
┌─────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client    │───▶│  POST /trans..  │───▶│  Kafka Topic    │
│ Application │    │ (with JWT Auth) │    │ transaction-    │
│ (with JWT)  │    │                 │    │   requests      │
└─────────────┘    └─────────────────┘    └─────────────────┘
                            │                       │
                            ▼                       ▼
       ┌─────────────────────────────────┐    ┌─────────────────┐
       │    Exchange Rate Service        │───▶│ Stream Processor│
       │  (Currency Conversion Logic)    │    │ (with Monthly   │
       │                                 │    │  Filtering)     │
       └─────────────────────────────────┘    └─────────────────┘
                    ▲                                   │
                    │                                   ▼
       ┌─────────────────────────────────┐    ┌─────────────────┐
       │     External Rate Provider      │    │ GET /trans..    │
       │    (Real-time Exchange Rates)   │    │ (Paginated with │
       │                                 │    │  Summary Data)  │
       └─────────────────────────────────┘    └─────────────────┘
                                                       │
                                              ┌─────────────────┐
                                              │   JWT Auth      │
                                              │  Validation     │
                                              └─────────────────┘
```

## System Requirements

### Runtime Requirements
- Java 17+
- Apache Kafka 3.0+ (Single Node)
- Spring Boot 3.0+
- Maven 3.8+

### Dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-streams</artifactId>
    </dependency>
</dependencies>
```

## Getting Started

### 1. Prerequisites
```bash
# Start single-node Kafka
docker-compose up -d kafka zookeeper

# Create topics with single partition and no replication
kafka-topics --create --topic transaction-requests --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092
kafka-topics --create --topic transaction-events --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092

# Verify topics are created
kafka-topics --list --bootstrap-server localhost:9092
```

### 2. Build and Run
```bash
# Clone repository
git clone <repository-url>
cd transactions-svc

# Build application
mvn clean package

# Run application
mvn spring-boot:run
```

### 3. Health Check
```bash
curl http://localhost:8080/actuator/health
```

### 4. Test Endpoints
```bash
# Get JWT token first (implementation depends on your auth provider)
export JWT_TOKEN="your_jwt_token_here"

# Test GET endpoint
curl -H "Authorization: Bearer $JWT_TOKEN" \
     "http://localhost:8080/api/v1/transactions?year=2024&month=8&page=0&size=10&baseCurrency=IDR"

# Test POST endpoint
curl -X POST \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "id": "89d3o179-abcd-465b-o9ee-e2d5f6ofEld46",
       "amount": 100.50,
       "currency": "GBP",
       "accountIban": "CH93-0000-0000-0000-0000-0",
       "valueDate": "2024-08-19",
       "description": "Online payment CHF",
       "customerId": "P-0123456789",
       "type": "DEBIT"
     }' \
     http://localhost:8080/api/v1/transactions
```

## Configuration

### Application Properties
```yaml
server:
  port: 8080

spring:
  application:
    name: transactions-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: transactions-consumer
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

logging:
  level:
    com.banking.transactions: DEBUG
```

### Environment Variables
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka cluster connection string
- `EXCHANGE_RATE_API_URL`: External exchange rate API URL
- `EXCHANGE_RATE_API_KEY`: API key for exchange rate service
- `JWT_ISSUER_URI`: JWT token issuer URI for validation
- `JWT_SECRET`: JWT secret key for token validation

## Deployment

### Docker Compose for Single-Node Kafka

```yaml
services:
  kafka:
    image: apache/kafka:4.1.0-rc2
    container_name: kafka
    ports:
      - 9092:9092
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://localhost:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_NUM_PARTITIONS: 3
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transactions-svc
spec:
  replicas: 3
  selector:
    matchLabels:
      app: transactions-svc
  template:
    metadata:
      labels:
        app: transactions-svc
    spec:
      containers:
      - name: transactions-svc
        image: transactions-svc:latest
        ports:
        - containerPort: 8080
        env:
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-headless:9092"
```

## Monitoring

### Metrics Endpoints
- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Kafka Streams: `GET /actuator/metrics/kafka.stream.*`

### Key Metrics to Monitor
- Transaction throughput (transactions/second)
- Currency conversion accuracy
- JWT authentication success/failure rates
- Monthly transaction volumes by currency
- Exchange rate API response time and error rates
- Kafka consumer lag
- Page summary calculation performance

### Logging
The service uses structured logging with the following levels:
- `ERROR`: System errors, failed transactions
- `WARN`: Exchange rate API failures, validation errors
- `INFO`: Transaction processing events, startup/shutdown
- `DEBUG`: Detailed request/response logging

## Architecture Decisions

### 1. Event-Driven Architecture
**Decision**: Use Kafka for asynchronous transaction processing
**Rationale**:
- Enables horizontal scaling
- Provides fault tolerance and message durability
- Supports real-time streaming capabilities

### 2. Microservice Pattern
**Decision**: Separate transaction processing from other financial services
**Rationale**:
- Single responsibility principle
- Independent deployment and scaling
- Technology diversity support

### 3. External Exchange Rate Integration
**Decision**: Use external API for exchange rates
**Rationale**:
- Real-time accuracy
- Reduced complexity
- Professional-grade data source

### Single-Node Configuration Notes

**Important Considerations for Single-Node Kafka:**

1. **No Fault Tolerance**: With replication factor of 1, there's no data redundancy
2. **Simplified Partitioning**: Single partition means no parallel processing within topics
3. **Development/Testing**: Ideal for local development and testing environments
4. **Resource Requirements**: Lower memory and CPU requirements compared to multi-node setup

**Production Considerations:**
- For production environments, consider multi-node setup for high availability
- Monitor disk usage as all data is stored on a single node
- Implement proper backup strategies for data persistence

### Performance Implications
- **Throughput**: Limited by single broker performance
- **Scaling**: Vertical scaling only (more CPU/RAM to single node)
- **Ordering**: Messages maintain strict order within each topic

