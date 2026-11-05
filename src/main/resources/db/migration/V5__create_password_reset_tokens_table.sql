-- ==============================================================================
-- Flyway Migration V5: Create Password Reset Tokens Table
-- Assignment 2: Email-based Password Reset & Account Verification
-- ==============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(160) NOT NULL,
    token_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_email ON password_reset_tokens (email);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_code ON password_reset_tokens (token_code);

