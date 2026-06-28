ALTER TABLE item
    ADD COLUMN drop_product BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN drop_starts_at DATETIME(6) NULL,
    ADD COLUMN drop_ends_at DATETIME(6) NULL,
    ADD COLUMN drop_purchase_limit INT NULL;
