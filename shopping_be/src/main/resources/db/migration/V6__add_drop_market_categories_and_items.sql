INSERT INTO category (name)
SELECT '신발'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '신발');

INSERT INTO category (name)
SELECT '가방'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '가방');

INSERT INTO category (name)
SELECT '모자'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '모자');

INSERT INTO category (name)
SELECT '시계'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '시계');

INSERT INTO category (name)
SELECT '키즈'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '키즈');

INSERT INTO category (name)
SELECT '액세서리'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '액세서리');

INSERT INTO item (
    deleted,
    price,
    quantity,
    category_id,
    image_url,
    item_name,
    drop_product,
    drop_starts_at,
    drop_ends_at,
    drop_purchase_limit
)
SELECT
    0,
    89000,
    6,
    c.id,
    'https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg?auto=compress&cs=tinysrgb&w=1200',
    '레드 러너 스니커즈 드롭',
    1,
    DATE_SUB(NOW(6), INTERVAL 1 DAY),
    DATE_ADD(NOW(6), INTERVAL 365 DAY),
    1
FROM category c
LEFT JOIN item existing_item ON existing_item.item_name = '레드 러너 스니커즈 드롭'
WHERE c.name = '신발'
  AND existing_item.id IS NULL;

INSERT INTO item (
    deleted,
    price,
    quantity,
    category_id,
    image_url,
    item_name,
    drop_product,
    drop_starts_at,
    drop_ends_at,
    drop_purchase_limit
)
SELECT
    0,
    119000,
    7,
    c.id,
    'https://images.pexels.com/photos/36367484/pexels-photo-36367484.jpeg?auto=compress&cs=tinysrgb&w=1200',
    '오벌 버킷 토트백 드롭',
    1,
    DATE_ADD(NOW(6), INTERVAL 7 DAY),
    DATE_ADD(NOW(6), INTERVAL 8 DAY),
    1
FROM category c
LEFT JOIN item existing_item ON existing_item.item_name = '오벌 버킷 토트백 드롭'
WHERE c.name = '가방'
  AND existing_item.id IS NULL;

INSERT INTO item (
    deleted,
    price,
    quantity,
    category_id,
    image_url,
    item_name,
    drop_product,
    drop_starts_at,
    drop_ends_at,
    drop_purchase_limit
)
SELECT
    0,
    39000,
    10,
    c.id,
    'https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1200',
    '옐로우 볼캡 선착순 드롭',
    1,
    DATE_SUB(NOW(6), INTERVAL 1 DAY),
    DATE_ADD(NOW(6), INTERVAL 365 DAY),
    2
FROM category c
LEFT JOIN item existing_item ON existing_item.item_name = '옐로우 볼캡 선착순 드롭'
WHERE c.name = '모자'
  AND existing_item.id IS NULL;

INSERT INTO item (
    deleted,
    price,
    quantity,
    category_id,
    image_url,
    item_name,
    drop_product,
    drop_starts_at,
    drop_ends_at,
    drop_purchase_limit
)
SELECT
    0,
    189000,
    4,
    c.id,
    'https://images.pexels.com/photos/9713527/pexels-photo-9713527.jpeg?auto=compress&cs=tinysrgb&w=1200',
    '스틸 클래식 워치 드롭',
    1,
    DATE_ADD(NOW(6), INTERVAL 7 DAY),
    DATE_ADD(NOW(6), INTERVAL 8 DAY),
    1
FROM category c
LEFT JOIN item existing_item ON existing_item.item_name = '스틸 클래식 워치 드롭'
WHERE c.name = '시계'
  AND existing_item.id IS NULL;

INSERT INTO item (
    deleted,
    price,
    quantity,
    category_id,
    image_url,
    item_name,
    drop_product,
    drop_starts_at,
    drop_ends_at,
    drop_purchase_limit
)
SELECT
    0,
    59000,
    12,
    c.id,
    'https://images.pexels.com/photos/37451174/pexels-photo-37451174.jpeg?auto=compress&cs=tinysrgb&w=1200',
    '키즈 스트리트 셋업',
    0,
    NULL,
    NULL,
    NULL
FROM category c
LEFT JOIN item existing_item ON existing_item.item_name = '키즈 스트리트 셋업'
WHERE c.name = '키즈'
  AND existing_item.id IS NULL;

INSERT INTO item (
    deleted,
    price,
    quantity,
    category_id,
    image_url,
    item_name,
    drop_product,
    drop_starts_at,
    drop_ends_at,
    drop_purchase_limit
)
SELECT
    0,
    49000,
    15,
    c.id,
    'https://images.pexels.com/photos/2986445/pexels-photo-2986445.jpeg?auto=compress&cs=tinysrgb&w=1200',
    '썸머 액세서리 세트',
    0,
    NULL,
    NULL,
    NULL
FROM category c
LEFT JOIN item existing_item ON existing_item.item_name = '썸머 액세서리 세트'
WHERE c.name = '액세서리'
  AND existing_item.id IS NULL;
