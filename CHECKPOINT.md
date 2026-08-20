# CHECKPOINT — notification-silo

_Last updated: 2026-08-20. This document is the arbiter: if Claude asserts something about this repo that isn't here or visible in the code, call it — that's drift._

**Role in the platform:** consumer. Subscribes to `user-silo.user-registered` (Phase 5: inbox dedup, notification record + structured log). Resource server for any HTTP surface. Base package `io.github.siloverse.notification`.

## Built & green (Phase 2, 2026-08-20)

- Resource server against realm `kyc`: same stack as user-silo — `SecurityConfiguration` + `KeycloakJwtAuthenticationConverter` as deliberate verbatim copies (copy #3 of 3; the rule-of-three threshold is now MET — extraction to java-library `security-spring` is justified whenever Phase 8.6 arrives). Shared rationale + lessons: see user-silo/CHECKPOINT.md.
- Verified: no token → 401, garbage → 401, user token → `ROLE_customer`, SA token → `ROLE_system`.

## Parked

- `/api/users/me` exists here only as a chain test — this silo has no identity surface; delete when Phase 5 gives it real endpoints (if any).
- Tests (plan 2.6).
