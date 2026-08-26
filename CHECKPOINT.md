# CHECKPOINT — notification-silo

_Last updated: 2026-08-20. This document is the arbiter: if Claude asserts something about this repo that isn't here or visible in the code, call it — that's drift._

**Role in the platform:** consumer. Subscribes to `user-silo.user-registered` (Phase 5: inbox dedup, notification record + structured log). Resource server for any HTTP surface. Base package `io.github.siloverse.notification`.

## Built & green (Phase 5 consumer, 2026-08-26) — the silo does its job

- **`UserRegisteredNotifier`** (`@Consumer(id = "user-registered-notifier", dedup = true)`): inserts a `WELCOME` row into `notification_silo.notifications` + one structured log line. Inbox ledger and business INSERT commit in ONE transaction (library-started) — at-least-once delivery, exactly-once effect.
- Persistence: `NotificationEntity` (route-B explicit schema, app-assigned UUID ctor default) + `NotificationRepository`; `V2__notifications.sql`; `kotlin("plugin.jpa")` added to this silo (convention adoption still parked).
- **Golden-path integration test** (`GoldenPathIntegrationTest`, Testcontainers postgres + rabbitmq): envelope on the wire → notification row; SAME messageId redelivered → still one row (dedup proven); fresh messageId → second row. This test found library finding #6 (unqualified table names) — see java-library/CHECKPOINT.md.
- Live end-to-end verified 2026-08-26: registration via auth-silo → `user_silo.messaging_outbox` stamped → RabbitMQ vhost `siloverse` → notification row + inbox ledger entry here.
- Lesson paid: migrations lived in a single directory literally named `db.migration` (IDE new-directory trap) — Flyway ran, found nothing, created only its history table. `db/migration`, two nested directories, always.
- Config notes: vhost `siloverse` (platform name, like the shared DB — macgrant hiera updated); dev creds committed by decision (Vault = task 8.3).

## Built & green (Phase 2, 2026-08-20)

- Resource server against realm `kyc`: same stack as user-silo — `SecurityConfiguration` + `KeycloakJwtAuthenticationConverter` as deliberate verbatim copies (copy #3 of 3; the rule-of-three threshold is now MET — extraction to java-library `security-spring` is justified whenever Phase 8.6 arrives). Shared rationale + lessons: see user-silo/CHECKPOINT.md.
- Verified: no token → 401, garbage → 401, user token → `ROLE_customer`, SA token → `ROLE_system`.

## Parked

- `/api/users/me` exists here only as a chain test — this silo has no identity surface; delete when Phase 5 gives it real endpoints (if any).
- Tests (plan 2.6).
