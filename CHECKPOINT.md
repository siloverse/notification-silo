# CHECKPOINT — notification-silo

_Last updated: 2026-10-11. This document is the arbiter: if Claude asserts something about this repo that isn't here or visible in the code, call it — that's drift._

**Role in the platform:** consumer. Subscribes to `user-silo.user-registered` (Phase 5: inbox dedup, notification record + structured log). Resource server for any HTTP surface. Base package `io.github.siloverse.notification`.

## State at merge to main (2026-10-11)

- Branch `add-spring-secuirty` (2026-08-19 to 2026-09-03) merged: structure, release 0.0.1, resource server (Phase 2), the `UserRegistered` consumer with inbox dedup and notification table (Phase 5), observability (Phase 6), messaging 1.0.11 tracing, retry tiers with `@RabbitPolicy` verified by a live poison test, siloverse-build 1.11.1.
- **Released:** `0.0.1`. Nothing depends on this silo's artifacts; no release is pending. Version is `0.0.2-SNAPSHOT`.
- Pins messaging `1.0.11-SNAPSHOT` and user-silo `messages` `1.1.4-SNAPSHOT` from mavenLocal; CI will be red until both are released and pinned.
- Still parked: the `/api/users/me` chain-test leftover (`MeController` is still here), tests for the HTTP surface (plan 2.6).

## Retry tiers live (2026-08-31) — poison test verified on the real broker

- **Wiring** (`MessagingConfiguration.kt`): `retrySettings` bean binds `messaging.rabbitmq.default-policy` → `RetryPolicy` via Binder, `.map { RetrySettings(it) }.orElseGet { RetrySettings.DEFAULT }`; passed to `declareConsumerTopology`; `failedDeliveryHandler` bean (interface-typed) = `RetryingFailedDeliveryHandler(retrySettings, "notification-silo")`, handed to the listener after the inbox. Nullable creep removed while in there (`ConsumerRegistry`/`MessageNameRegistry` params de-nulled too).
- **Per-consumer policy in code**: `@RabbitPolicy(maxAttempts = 10, delayMillis = 5_000)` on the notifier, next to `@Consumer` — the library's annotation>yml>code-default ladder (see java-library/CHECKPOINT.md for the design story). application.yml deliberately has NO `default-policy` block (YAGNI: single consumer, annotated; code default `RetrySettings.DEFAULT` (3, 10s) is the silo default until a second consumer needs otherwise).
- **Broker migration paid**: old argument-less queue deleted in vhost `siloverse`, restart declared dlq → retry → main with dead-letter args. Reminder for next time: changing `delayMillis` re-declares the retry queue TTL → 406; queue deletion is part of that deploy.
- **Live poison test — every prediction verified**: retry queue `x-message-ttl: 5000` (the annotation, on the broker); `x-death` = 9× rejected (main) + 9× expired (retry); parked on delivery 10 = exactly `maxAttempts`; exactly ONE DLQ message carrying `x-parking-cause: RuntimeException: Poison Message`; parking republish added no x-death entry (counter stays honest); `traceparent` survived all nine round trips — Tempo trace `3a338553323c64a65004f49ee38dd6c7` = the poison money shot (~20 red consume spans, 10 deliveries × 2 invocations via the immediate in-process retry, ~50s timeline). Test resident may still sit in the .dlq (purge or keep as museum piece — DLQ-with-a-human means Awais is the human).

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
