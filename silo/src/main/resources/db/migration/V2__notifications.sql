-- The consumer's business record: one row per notification the platform owes/sent a user.
-- Inserted inside the same transaction as the messaging_inbox ledger row, so a redelivered
-- user-registered event can never produce a second WELCOME notification.
CREATE TABLE notification_silo.notifications
(
    id              UUID PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    kind            VARCHAR(64)  NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
