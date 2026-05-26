ALTER TABLE verification_tokens
    DROP CONSTRAINT verification_tokens_type_check;

ALTER TABLE verification_tokens
    ADD CONSTRAINT verification_tokens_type_check
        CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'PASSWORD_RESET_CONFIRMATION'));
