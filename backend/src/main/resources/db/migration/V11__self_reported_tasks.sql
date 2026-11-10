-- V11: Self-reported tasks and manager email preference

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS self_reported BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE manager_email_prefs
    ADD COLUMN IF NOT EXISTS on_self_reported BOOLEAN NOT NULL DEFAULT TRUE;
