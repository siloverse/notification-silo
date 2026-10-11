# notification-silo

The consumer in the siloverse platform. Spring Boot 4 on Kotlin, Java 21. Listens on
port `10201` and is reachable at `https://notification-silo.macgrant-platform.test`
through the macgrant gateway.

## What it does

Subscribes to `user-silo.user-registered` on RabbitMQ vhost `siloverse` and, for each
registration, inserts a `WELCOME` row into `notification_silo.notifications` and writes
one structured log line.

- `@Consumer(id = "user-registered-notifier", dedup = true)`: the inbox ledger entry and
  the business insert commit in one transaction, so delivery is at-least-once and the
  effect is exactly-once.
- `@RabbitPolicy(maxAttempts = 10, delayMillis = 5_000)`: constant-delay retry tiers
  (dlq, retry, main queues). A poison message is parked in the dead-letter queue after
  exactly ten attempts with an `x-parking-cause` header, trace context intact.
- Resource server against Keycloak realm `kyc` for any HTTP surface. `GET /api/users/me`
  remains only as a chain-test leftover.

## Persistence

Flyway migrations in `silo/src/main/resources/db/migration`: `V1` the messaging inbox
ledger, `V2` the notifications table. Schema `notification_silo` in the shared
`siloverse` database, named explicitly everywhere; Hibernate only validates.

## Observability

Traces and logs go to the macgrant OpenTelemetry collector over OTLP; metrics are
scraped by the platform's Prometheus from `/actuator/prometheus`. The consumer span joins
the trace started in auth-silo, with the broker gap visible.

## Running

1. Bring the platform up (`vagrant up` in macgrant).
2. `./gradlew :silo:bootRun`.
3. Tests: `./gradlew build`. The golden-path integration test runs Postgres and RabbitMQ
   in Testcontainers and proves dedup over the real broker.

Changing `delayMillis` re-declares the retry queue TTL, which the broker refuses (406);
deleting the queues is part of that deploy.

## Dependencies

- `io.github.siloverse:messaging-*` from java-library, pinned to `1.0.11-SNAPSHOT`.
- user-silo `messages` contract jar, pinned to `1.1.4-SNAPSHOT`.
  Both come from `mavenLocal()` until messaging 1.0.11 and user-silo 1.1.4 are released.
- Build conventions from siloverse-build 1.11.1.

`CHECKPOINT.md` is the engineering journal and the arbiter of what is decided and built.
