ALTER TABLE payment
    DROP CONSTRAINT chk_payment_provider;

ALTER TABLE payment
    ADD CONSTRAINT chk_payment_provider
        CHECK (provider IN ('MOCK', 'TOSS'));
