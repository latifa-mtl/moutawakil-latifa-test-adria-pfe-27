# Bank Platform - Microservices

Banking platform built with a microservices architecture. It allows consulting bank accounts, recording transactions and transfers, and exposes the business services through REST, GraphQL and MCP.

Technical test: ADRIA PFE 27.

## Technology stack

| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3 |
| Spring AI (MCP server) | 2.0.1 |
| Build tool | Maven (multi-module) |
| Database | H2 (in memory, one database per service) |
| API documentation | springdoc OpenAPI (Swagger UI) |

## Architecture

```
                        +----------------------+
         Client  -----> |   gateway-service    |
                        |  (Spring Cloud GW)   |
                        +----------+-----------+
                                   | lb://
                  +----------------+----------------+
                  |                                 |
        +---------v----------+            +---------v-----------+
        |   account-service  | <--------- | transaction-service |
        |  REST/GraphQL/MCP  |   OpenFeign|  REST/GraphQL/MCP   |
        +---------+----------+ Resilience4J+---------+----------+
                  |                                  |
                  +----------+          +------------+
                             v          v
                        +---------------------+
                        |  discovery-service  |
                        |   (Eureka Server)   |
                        +---------------------+
```

Each business service registers itself in Eureka. The gateway and the OpenFeign client resolve services by name through the registry.

## Modules

| Module | Port | Responsibility |
|---|---|---|
| `discovery-service` | 8761 | Eureka server: service registry and discovery |
| `gateway-service` | 8080 | Spring Cloud Gateway: single entry point and routing |
| `account-service` | 8081 | Bank accounts management (REST, GraphQL, MCP) |
| `transaction-service` | 8082 | Transactions and transfers (REST, GraphQL, MCP), OpenFeign client, Resilience4J |

Business services follow the same layered structure:

```
domain / entities     JPA entities
repository            Spring Data repositories
service               business logic (single entry point for all connectors)
web/rest              REST controllers
web/graphql           GraphQL controllers
mcp                   MCP tools for the AI agent
```

REST, GraphQL and MCP controllers all call the same service layer.

## Business rules

- The amount of a transaction must be strictly positive and must not exceed `bank.transaction.max-amount` (default 1000).
- The account must exist and be `ACTIVE`.
- The transaction currency must match the account currency.
- Each new transaction requires the current account balance, which is read from account-service. After the transaction is saved, the account balance is updated.
- A transfer debits a source account and credits a target account. If the credit fails, the debit is compensated.

## Prerequisites

- JDK 21
- Maven 3.9 or later
- Free ports: 8761, 8080, 8081, 8082

## Build

```bash
mvn clean install
```

## Run

Start the services in this order, each one in its own terminal:

```bash
mvn -pl discovery-service spring-boot:run
mvn -pl account-service spring-boot:run
mvn -pl transaction-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

Each service can also be started from the IDE through its main class.

## Discovery service (Eureka)

Configuration (`discovery-service/src/main/resources/application.properties`):

```properties
spring.application.name=discovery-service
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

Business services register with:

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.instance.prefer-ip-address=true
```

Dashboard: http://localhost:8761

## Gateway service

The gateway is the single entry point of the platform and routes requests to the services registered in Eureka using the `lb://` scheme.

Configuration (`gateway-service/src/main/resources/application.yml`):

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: account-service
              uri: lb://account-service
              predicates:
                - Path=/accounts/**
            - id: transaction-service
              uri: lb://transaction-service
              predicates:
                - Path=/transactions/**
            - id: account-service-extras
              uri: lb://account-service
              predicates:
                - Path=/account-api/**
              filters:
                - StripPrefix=1
            - id: transaction-service-extras
              uri: lb://transaction-service
              predicates:
                - Path=/transaction-api/**
              filters:
                - StripPrefix=1

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

Both business services expose their GraphQL, MCP and Swagger endpoints on identical paths, so these are reached through a service prefix that the gateway removes before forwarding.

| Gateway URL | Forwarded to |
|---|---|
| `/accounts/**` | account-service `/accounts/**` |
| `/transactions/**` | transaction-service `/transactions/**` |
| `/account-api/graphql` | account-service `/graphql` |
| `/account-api/mcp` | account-service `/mcp` |
| `/transaction-api/graphql` | transaction-service `/graphql` |
| `/transaction-api/mcp` | transaction-service `/mcp` |

## Service endpoints

### account-service (http://localhost:8081)

| Method | Path | Description |
|---|---|---|
| GET | `/accounts` | List all accounts |
| GET | `/accounts/{id}` | Get an account |
| POST | `/accounts?customerReference=&currency=&initialBalance=` | Create an account |
| PATCH | `/accounts/{id}/status?status=ACTIVE\|BLOCKED` | Change account status |
| PATCH | `/accounts/{id}/balance?delta=` | Apply a balance variation (negative for a debit) |

### transaction-service (http://localhost:8082)

| Method | Path | Description |
|---|---|---|
| GET | `/transactions` | List transactions, optional `accountId` filter |
| GET | `/transactions/{id}` | Get a transaction |
| POST | `/transactions?accountId=&amount=&currency=&merchantCategory=&country=` | Record a transaction |
| POST | `/transactions/transfers?sourceAccountId=&targetAccountId=&amount=&currency=` | Transfer between two accounts |

### GraphQL

Endpoint `/graphql` on each service, with the GraphiQL console at `/graphiql`.

```graphql
query { accounts { id customerReference status currency balance } }

mutation {
  createTransaction(accountId: "<uuid>", amount: 50, currency: "EUR",
                    merchantCategory: "FOOD", country: "MA") { id amount createdAt }
}
```

### MCP

MCP servers use the streamable HTTP transport and are available at `/mcp` on each service.

| Service | Tools |
|---|---|
| account-service | `list_accounts`, `get_account`, `create_account`, `update_account_status` |
| transaction-service | `list_transactions`, `list_account_transactions`, `create_transaction`, `transfer_money` |

## Fault tolerance

`transaction-service` calls `account-service` through an OpenFeign client protected by a Resilience4J circuit breaker.

```properties
spring.cloud.openfeign.client.config.default.connect-timeout=2000
spring.cloud.openfeign.client.config.default.read-timeout=3000

resilience4j.circuitbreaker.instances.accountService.sliding-window-size=10
resilience4j.circuitbreaker.instances.accountService.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.accountService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.accountService.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.accountService.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.accountService.automatic-transition-from-open-to-half-open-enabled=true
```

| Mechanism | Behaviour |
|---|---|
| Timeouts | 2 seconds to connect, 3 seconds to read |
| Circuit breaker | Opens at 50 percent failures over the last 10 calls (minimum 5), stays open 10 seconds, then allows 3 trial calls |
| Fallback | Raises an explicit error; no balance or account data is ever invented |
| Transaction | The local database write is rolled back if the balance update fails |

## Configuration

| Property | Default | Description |
|---|---|---|
| `bank.transaction.max-amount` | 1000 | Maximum amount of a single transaction |
| `bank.account-service.url` | `http://localhost:8081` | Direct URL of account-service. Leave empty to use Eureka discovery |

## Test scenarios

Replace `<id>` values with identifiers returned by `GET /accounts`. Account identifiers are regenerated at each restart because the databases are in memory.

### Discovery

1. Open http://localhost:8761 and check that `ACCOUNT-SERVICE`, `TRANSACTION-SERVICE` and `GATEWAY-SERVICE` are listed with status UP.
2. Stop account-service and check that it disappears from the dashboard after a few seconds.

### Gateway

```bash
curl http://localhost:8080/accounts
curl http://localhost:8080/transactions
curl -X POST "http://localhost:8080/transactions?accountId=<id>&amount=250&currency=EUR&merchantCategory=GROCERY&country=FR"
curl -X POST "http://localhost:8080/transactions/transfers?sourceAccountId=<id1>&targetAccountId=<id2>&amount=100&currency=EUR"
curl -X POST http://localhost:8080/account-api/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ accounts { id status balance } }"}'
```

Expected: each call returns the same result as when calling the service directly, which confirms that routing through the registry works.

### Business rules

| Case | Request | Expected result |
|---|---|---|
| Zero amount | `amount=0` | Rejected: amount must be strictly positive |
| Above maximum | `amount=1500` | Rejected: maximum is 1000 |
| Blocked account | account with status BLOCKED | Rejected: account is BLOCKED |
| Currency mismatch | `currency=USD` on an EUR account | Rejected: currency does not match |
| Insufficient balance | amount above the balance | Rejected: insufficient balance |
| Unknown account | random UUID | Rejected: account refused or not found |

### Fault tolerance

1. Create a transaction successfully.
2. Stop account-service.
3. Repeat `POST /transactions` five to six times. The first calls return `account-service unavailable`. Once the circuit is open, the response is immediate and mentions that the circuit breaker is OPEN.
4. Restart account-service and wait 10 seconds. Calls succeed again.
5. Check in the H2 console of transaction-service that no transaction was stored during the outage.

## Tools

| Tool | URL |
|---|---|
| Eureka dashboard | http://localhost:8761 |
| Swagger UI, account-service | http://localhost:8081/swagger-ui/index.html |
| Swagger UI, transaction-service | http://localhost:8082/swagger-ui/index.html |
| H2 console, account-service | http://localhost:8081/h2-console (`jdbc:h2:mem:accounts-db`, user `sa`, empty password) |
| H2 console, transaction-service | http://localhost:8082/h2-console (`jdbc:h2:mem:transactions-db`, user `sa`, empty password) |
| GraphiQL | `/graphiql` on each service |

## Project structure

```
bank-platform/
  pom.xml
  discovery-service/
  gateway-service/
  account-service/
  transaction-service/
```

## Roadmap

| Part | Content | Status |
|---|---|---|
| B | Class diagram and architecture schema | Done |
| C | account-service, transaction-service, OpenFeign and Resilience4J | Done |
| D | Gateway, Eureka, Config service (`bank.transaction.max-amount=1000`) | Gateway and Eureka done, Config service pending |
| E | Agentic chatbot (Spring AI, MCP tools) | Pending |
| F | Kafka events on `bank.transactions.created` | Pending |
| G | Web client | Pending |
| H | Fraud detection notebook | Pending |
| I | Docker, Docker Compose, CI/CD | Pending |
