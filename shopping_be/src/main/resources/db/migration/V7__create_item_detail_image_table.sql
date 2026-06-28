CREATE TABLE IF NOT EXISTS item_detail_image (
  id BIGINT NOT NULL AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  display_order INT NOT NULL,
  image_role VARCHAR(30) NOT NULL,
  image_url VARCHAR(1024) NOT NULL,
  alt_text VARCHAR(255) NOT NULL,
  caption VARCHAR(255) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_item_detail_image_item
    FOREIGN KEY (item_id) REFERENCES item (id)
    ON DELETE CASCADE,
  CONSTRAINT uk_item_detail_image_item_order
    UNIQUE (item_id, display_order),
  INDEX idx_item_detail_image_item_order (item_id, display_order),
  CONSTRAINT chk_item_detail_image_role
    CHECK (image_role IN ('MOOD', 'DETAIL'))
);
