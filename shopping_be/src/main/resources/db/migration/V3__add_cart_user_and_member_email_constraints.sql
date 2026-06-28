UPDATE member
SET email = CONCAT('deleted__', id, '__', REPLACE(UUID(), '-', ''), '@deleted.local')
WHERE active = false;

ALTER TABLE cart
    MODIFY COLUMN user_id BIGINT NOT NULL;

ALTER TABLE cart
    ADD CONSTRAINT uk_cart_user UNIQUE (user_id);

ALTER TABLE member
    ADD CONSTRAINT uk_member_email UNIQUE (email);
