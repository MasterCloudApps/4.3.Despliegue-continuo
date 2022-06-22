-- This change is backward incompatible - you can't do A/B testing
ALTER TABLE person RENAME COLUMN last_name TO surname;