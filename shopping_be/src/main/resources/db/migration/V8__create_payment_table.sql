ALTER TABLE orders
    MODIFY COLUMN status VARCHAR(30) NULL;

CREATE TABLE IF NOT EXISTS payment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  provider VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL,
  payment_order_id VARCHAR(80) NOT NULL,
  provider_payment_key VARCHAR(120) NULL,
  amount INT NOT NULL,
  requested_at DATETIME(6) NOT NULL,
  approved_at DATETIME(6) NULL,
  failure_reason VARCHAR(255) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_payment_order
    FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT uk_payment_order
    UNIQUE (order_id),
  CONSTRAINT uk_payment_order_id
    UNIQUE (payment_order_id),
  INDEX idx_payment_order_id (payment_order_id),
  INDEX idx_payment_status (status),
  CONSTRAINT chk_payment_provider
    CHECK (provider IN ('MOCK')),
  CONSTRAINT chk_payment_status
    CHECK (status IN ('READY', 'APPROVED', 'FAILED', 'CANCELLED'))
);
