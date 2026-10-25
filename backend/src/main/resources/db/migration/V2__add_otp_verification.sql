-- FindIt V2 Migration: Add OTP and Verification Columns
-- Compatible with MySQL 8.0+ and H2 (MySQL Mode)

ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN otp_code VARCHAR(10);
ALTER TABLE users ADD COLUMN otp_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN pending_verification_target VARCHAR(120);
