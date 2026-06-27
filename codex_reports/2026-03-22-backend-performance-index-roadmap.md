# Shopping 백엔드 성능 테스트 및 DB 인덱스 적용 로드맵

## 1. 이 문서의 목적

이 문서는 현재 `shopping_be` 프로젝트를 기준으로, "언제 인덱스를 걸어야 하는지", "무슨 도구로 성능 테스트를 해야 하는지", "테스트 결과를 보고 어떤 인덱스를 어떤 순서로 적용할지"를 처음부터 끝까지 정리한 실행용 로드맵이다.

이 문서는 설명용 이론이 아니라 현재 코드베이스의 실제 조건을 반영한다.

- 백엔드: Spring Boot 3.5.5, Java 21, Spring MVC, Spring Data JPA, Redis, Flyway
- 실제 DB: MySQL
- 테스트 DB: H2
- 인증: JWT + HttpOnly Cookie + Redis Refresh Token
- 현재 프로젝트에는 `k6` 스크립트는 없고, 시드 스크립트와 API 기반 데이터 생성 스크립트는 일부 존재한다

## 2. 먼저 질문한 개념 정리

### 2.1 `gender`, `is_deleted` 같은 컬럼은 왜 단독 인덱스가 약한가

핵심 이유는 맞다. 값 종류가 너무 적어서다.

- `gender`: 예를 들어 `M/F` 두 값 정도
- `is_deleted`, `active`, `deleted`: 사실상 `true/false` 두 값

이런 컬럼은 `선택도(selectivity)`가 낮다. 즉, 인덱스로 골라도 너무 많은 행이 같이 걸린다.  
예를 들어 전체 상품 100만 건 중 `deleted = false`가 97만 건이면, `deleted` 인덱스를 타도 결국 엄청 많은 행을 읽어야 하므로 이득이 작다.

그래서 이런 컬럼은 보통 아래처럼 쓴다.

- 단독 인덱스: 보통 비추천
- 복합 인덱스의 보조 컬럼: 경우에 따라 유효

예시:

```sql
-- 비추천 가능성이 큼
create index idx_item_deleted on item(deleted);

-- 더 현실적인 후보
create index idx_item_category_deleted_id on item(category_id, deleted, id);
```

즉, `deleted` 자체가 나빠서가 아니라, "단독으로는 너무 많은 행을 잡기 때문"이다.

### 2.2 쓰기가 많은데 조회 이득이 작은 경우는 어떤 경우인가

인덱스는 조회를 빠르게 하지만, 반대로 `INSERT/UPDATE/DELETE` 때마다 인덱스도 같이 관리해야 한다.  
그래서 아래 같은 컬럼은 "인덱스 유지 비용"이 "조회 이득"보다 커질 수 있다.

- `item.deleted`
  - 상품 삭제/복구 시 값이 바뀜
  - 그런데 대부분 조회가 `id`나 `category_id` 기준이라면 단독 인덱스 이득이 작다
- `member.active`
  - 탈퇴/비활성화 상태 용도
  - `active = true` 사용자가 대부분이면 단독 인덱스 효율이 낮다
- `orders.status`
  - `ORDERED`, `PAID`, `CANCELLED` 정도면 값 종류가 적다
  - 관리자 화면이 `status`만으로 자주 대량 조회하지 않으면 단독 인덱스 필요성이 낮다
- `item.quantity`
  - 재고 변경이 자주 일어날 수 있음
  - 보통 이 컬럼으로 직접 찾지 않고 `id`로 상품을 찾은 뒤 읽는다

정리하면, "자주 바뀌고", "값 종류가 적거나", "실제 조회 조건에 잘 안 쓰이는 컬럼"은 인덱스를 조심해서 걸어야 한다.

### 2.3 TEXT 타입은 인덱스를 쓰지 말라는 뜻인가

그 뜻은 아니다. 정확히는 "긴 텍스트 전체에 일반 B-Tree 인덱스를 무심코 걸지 말라"에 가깝다.

현재 프로젝트에서 `post.content`는 이미 `TEXT`로 변경되어 있다.

- 파일: `shopping_be/src/main/resources/db/migration/V4__modify_post_content_type_TEXT.sql`

주의점은 아래와 같다.

- `LIKE '%키워드%'` 검색에는 일반 인덱스가 거의 도움이 안 된다
- MySQL에서 `TEXT`는 전체 컬럼 일반 인덱스 대신 `prefix index`나 `FULLTEXT`를 고려한다
- 검색 기능이 없다면 굳이 인덱스를 만들 필요가 없다

예시:

```sql
-- prefix index: 앞부분 비교에는 쓸 수 있지만 전문 검색에는 한계가 큼
create index idx_post_title_prefix on post(title(100));

-- 전문 검색이 필요할 때
create fulltext index ftx_post_title_content on post(title, content);
```

현재 프로젝트는 `post.content`를 조건절로 검색하지 않는다.  
따라서 지금 시점에서는 `post.content` 인덱스는 만들지 않는 것이 맞다.

## 3. 현재 프로젝트에서 성능 테스트를 반드시 MySQL 기준으로 해야 하는 이유

현재 설정을 보면:

- `src/test/resources/application.yml`은 H2 메모리 DB
- `src/main/resources/application-dev.yml`, `application-prod.yml`은 MySQL

즉:

- H2는 기능 테스트용
- 인덱스 검증과 실행계획 검증은 MySQL에서만 의미가 있다

왜냐하면 H2와 MySQL은 아래가 다르다.

- 실행계획
- 인덱스 사용 방식
- 락 동작
- 통계 정보
- `EXPLAIN ANALYZE` 결과

따라서 이 프로젝트에서 인덱스 의사결정은 반드시 "MySQL + 실제와 비슷한 데이터 양"에서 해야 한다.

## 4. 이 프로젝트에서 추천하는 도구 조합

### 4.1 1순위: `k6`

이 프로젝트는 REST API가 명확하고, 로그인도 `/api/login` 한 번으로 쿠키를 받을 수 있어서 `k6`가 가장 잘 맞는다.

추천 이유:

- HTTP API 부하 테스트에 특화
- 스크립트가 간단함
- CI에 넣기 쉬움
- 로그인 후 쿠키 유지 시나리오 작성이 쉬움
- 프론트가 아니라 백엔드 API 성능 검증에 집중하기 좋음

### 4.2 DB 분석 도구: MySQL `EXPLAIN ANALYZE` + slow query log

부하 테스트만으로는 "왜 느린지"를 알기 어렵다.  
따라서 MySQL 쪽에서 같이 본다.

- `EXPLAIN ANALYZE`
  - 특정 쿼리가 실제로 어떤 실행계획을 탔는지 확인
- slow query log
  - 어떤 쿼리가 오래 걸렸는지 수집
- `performance_schema`
  - 동일 패턴 SQL의 누적 통계 확인

### 4.3 애플리케이션 지표: Spring Actuator

이 프로젝트는 이미 Actuator 의존성이 있다.

- 파일: `shopping_be/build.gradle`

즉, 낮은 비용으로 아래를 추가할 수 있다.

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/httpexchanges`

운영급 관측을 원하면 나중에 `micrometer-registry-prometheus`를 붙여 Prometheus/Grafana까지 확장하면 된다.  
하지만 1차 인덱스 검증 단계에서는 `k6 + MySQL 분석`만으로도 충분하다.

## 5. 현재 코드 기준으로 병목 후보 API와 쿼리

현재 리포지토리와 서비스 코드를 보면 아래 경로가 우선 후보다.

### 5.1 상품 목록/카테고리/검색

- `/api/items`
- `/api/items?categoryId=...`
- `/api/items?keyword=...`
- `/api/items?categoryId=...&keyword=...`
- `/api/categories`

관련 코드:

- `JpaItemRepository`
- `JpaCategoryRepository`
- `ItemService`
- `CategoryService`

주요 패턴:

- `item.deleted = false`
- `item.category.id = :categoryId`
- `item.itemName like '%keyword%'`
- 카테고리별 상품 수 집계
- 카테고리 대표 이미지 조회

### 5.2 게시글 목록/상세

- `/api/posts?sort=desc`
- `/api/posts/{id}`

관련 코드:

- `JpaPostRepository`
- `JpaCommentRepository`
- `PostService`

주요 패턴:

- `order by p.createdDate desc`
- `where p.id = :id`
- `where p.user = :user`
- 댓글은 `post_id` 기준으로 조인

### 5.3 장바구니/주문/마이페이지

- `/api/cart`
- `/api/cart/items/{itemId}`
- `/api/orders`
- `/api/myPage`

관련 코드:

- `JpaCartRepository`
- `JpaOrderRepository`
- `CartService`
- `OrderService`

주요 패턴:

- `where c.user = :user`
- `where o.user = :user`
- 주문 시 `item.id`로 재고 확인 + `PESSIMISTIC_WRITE`
- `order_item`, `cart_item`는 FK 기반 조인

### 5.4 로그인

- `/api/login`

관련 코드:

- `JpaUserRepository.findByEmail`
- `RefreshTokenService`
- `RedisRefreshTokenStore`

주요 패턴:

- `member.email = ?`
- refresh token은 Redis에서 조회

여기는 이미 `member.email` 유니크 제약이 있어 인덱스가 있다.  
즉 로그인 병목은 DB 인덱스보다 비밀번호 해시, Redis, 네트워크, 애플리케이션 처리시간 쪽일 가능성이 더 높다.

## 6. 지금 바로 의심되는 인덱스 후보

중요: 아래는 "후보"다.  
반드시 성능 테스트와 `EXPLAIN ANALYZE`로 검증 후 채택해야 한다.

### 6.1 이미 사실상 존재하는 인덱스

현재 DDL/마이그레이션 기준으로 이미 있는 것들:

- PK 전부
- FK 인덱스
  - `cart.user_id`
  - `cart_item.cart_id`
  - `cart_item.item_id`
  - `comment.post_id`
  - `comment.user_id`
  - `item.category_id`
  - `order_item.item_id`
  - `order_item.order_id`
  - `orders.user_id`
  - `post.user_id`
- UNIQUE
  - `member.email`
  - `cart.user_id`

즉, 기초 인덱스는 이미 있다.

### 6.2 우선 검토 후보 1: `post(created_date)`

근거:

- `/api/posts?sort=desc`가 게시판 메인 조회
- 현재 `JpaPostRepository.findAllByCreatedDateDesc()`가 정렬을 직접 사용
- `created_date` 인덱스가 없으면 데이터가 커질수록 정렬 비용이 커진다

후보 SQL:

```sql
create index idx_post_created_date on post(created_date);
```

우선순위: 높음

### 6.3 우선 검토 후보 2: `item(category_id, deleted, id)`

근거:

- 카테고리별 상품 조회
- 카테고리 카드의 상품 수 집계
- 카테고리 대표 이미지 조회에서 `category + deleted + min(id)` 패턴이 있음

후보 SQL:

```sql
create index idx_item_category_deleted_id on item(category_id, deleted, id);
```

장점:

- `category_id` 필터
- `deleted = false` 보조 조건
- 카테고리별 대표 이미지의 `min(id)` 계산 보조

우선순위: 높음

### 6.4 검토 후보 3: `comment(post_id, created_date)`

근거:

- 현재는 댓글 정렬이 명시적이지 않지만, 댓글이 많아지면 게시글 상세에서 보통 시간순 정렬 요구가 생긴다
- `post_id`만으로도 기본 FK 인덱스는 있으나, 시간순 정렬이 붙으면 복합 인덱스가 더 유리하다

후보 SQL:

```sql
create index idx_comment_post_created_date on comment(post_id, created_date);
```

우선순위: 중간

### 6.5 검토 후보 4: `orders(user_id, order_date)`

근거:

- 현재 `where o.user = :user` 조회가 있음
- 마이페이지/주문내역은 보통 최신순 정렬이 붙는다
- 현재는 `user_id` FK 인덱스가 있어 급하지는 않지만, 정렬이 붙으면 복합 인덱스가 좋아질 수 있다

후보 SQL:

```sql
create index idx_orders_user_order_date on orders(user_id, order_date);
```

우선순위: 중간

### 6.6 보류 후보: `item(item_name)`

현재 코드는 `like '%keyword%'` 패턴이다.

```sql
where i.item_name like '%키워드%'
```

이 패턴은 일반 B-Tree 인덱스를 거의 못 탄다.  
따라서 아래 둘 중 하나로 가야 한다.

- 지금처럼 소규모에서는 그냥 둔다
- 검색이 중요해지면 `FULLTEXT` 또는 검색엔진(Elasticsearch 등)을 검토한다

즉, 지금 단계에서 `item_name` 일반 인덱스를 추가해도 기대보다 효과가 작을 가능성이 높다.

### 6.7 보류 후보: `member(active)`, `item(deleted)`, `orders(status)`

이 컬럼들은 값 종류가 적고, 단독 조회 가치가 낮다.  
단독 인덱스는 기본적으로 보류한다.

## 7. 성능 테스트 로드맵: 처음부터 끝까지

## 7.1 0단계. 목표 수치 먼저 정하기

테스트 전에 "느린지 빠른지"를 숫자로 정해야 한다.

예시 목표:

- 상품 목록 `/api/items`
  - P95 200ms 이하
- 게시글 목록 `/api/posts?sort=desc`
  - P95 200ms 이하
- 장바구니 조회 `/api/cart`
  - P95 250ms 이하
- 주문 생성 `/api/orders`
  - P95 400ms 이하
- 에러율
  - 1% 미만

이 수치는 절대값이 아니라 출발점이다.  
중요한 것은 "인덱스 전/후 같은 조건에서 비교 가능하게" 만드는 것이다.

## 7.2 1단계. 성능 테스트 환경 분리

현재 `docker-compose.yml`과 `docker-compose.dev.yml`에는 Redis와 앱만 있고 MySQL이 없다.  
따라서 성능 테스트 전용 MySQL 환경을 별도로 둬야 한다.

권장 원칙:

- 로컬 개발 DB와 분리
- 운영과 같은 MySQL 메이저 버전 사용
- Redis도 같이 붙여서 인증 흐름 포함 테스트

권장 구성:

- app
- mysql
- redis
- k6

중요:

- 성능 테스트는 절대 H2로 하지 않는다
- 가능하면 운영과 비슷한 CPU, 메모리, MySQL 설정을 맞춘다

## 7.3 2단계. 테스트 데이터 볼륨 만들기

현재 시드 데이터는 너무 작다.  
상품 몇 개, 게시글 몇 개 수준으로는 인덱스 효과가 거의 보이지 않는다.

현재 확인된 스크립트:

- `scripts/seed_real_products.py`
- `scripts/seed_community_activity.py`

이 스크립트들은 유용하지만, 성능 테스트용으로는 데이터 양을 훨씬 늘려야 한다.

권장 최소 데이터 규모:

- `member`: 10,000건
- `item`: 50,000건 이상
- `post`: 100,000건 이상
- `comment`: 300,000건 이상
- `orders`: 100,000건 이상
- `order_item`: 300,000건 이상
- `cart_item`: 50,000건 이상

로컬 장비가 약하면 1차로 절반 수준부터 시작해도 된다.  
중요한 것은 "정렬, 조인, 범위 조회, 댓글 개수"가 충분히 커지는 것이다.

## 7.4 3단계. 관측 포인트 켜기

### 애플리케이션

추천 항목:

- Spring Actuator 활성화
- API 응답시간
- 에러율
- DB 커넥션 풀 사용량

### MySQL

필수 항목:

```sql
set global slow_query_log = 'ON';
set global long_query_time = 0.2;
set global log_output = 'TABLE';
```

이렇게 하면 200ms 이상 쿼리를 먼저 잡아낼 수 있다.  
환경에 따라 `long_query_time`은 `0.1 ~ 0.5` 사이에서 조정한다.

### JPA / Hibernate

현재 dev 설정은 SQL 로그가 꽤 자세하다.  
기능 확인에는 좋지만, 진짜 부하 테스트에서는 로그가 오히려 성능에 영향을 줄 수 있다.

따라서 부하 테스트 프로필에서는:

- SQL 로그 최소화
- slow query log 중심 수집

이 편이 더 정확하다.

## 7.5 4단계. `k6` 시나리오 작성

이 프로젝트에서 우선 작성할 시나리오는 아래 4개다.

### 시나리오 A. 비로그인 사용자 카탈로그 탐색

목적:

- 가장 트래픽이 많을 가능성이 높은 공개 조회 API 검증

흐름:

1. `GET /api/categories`
2. `GET /api/items`
3. `GET /api/items?categoryId=...`
4. `GET /api/items/{id}`
5. `GET /api/posts?sort=desc`
6. `GET /api/posts/{id}`

### 시나리오 B. 로그인 사용자 장바구니/주문

목적:

- 쿠키 로그인 + 장바구니 + 주문 전환 흐름 검증

흐름:

1. `POST /api/login`
2. `GET /api/cart`
3. `POST /api/cart/items/{itemId}`
4. `GET /api/cart`
5. `POST /api/orders`
6. `GET /api/myPage`

### 시나리오 C. 게시판 활동

목적:

- 게시글/댓글 증가에 따른 목록/상세 부하 검증

흐름:

1. `GET /api/posts?sort=desc`
2. `GET /api/posts/{id}`
3. 로그인 후 `POST /api/posts`
4. `POST /api/posts/{postId}/comments`

### 시나리오 D. 관리자 상품 등록/수정

목적:

- S3 업로드를 포함한 쓰기 API 병목 확인

이 시나리오는 DB 인덱스보다 파일 업로드와 외부 I/O 영향이 커서 우선순위는 낮다.

## 7.6 5단계. 베이스라인 측정

인덱스를 추가하기 전에 반드시 먼저 측정한다.

측정 항목:

- VU 수별 P50 / P95 / P99
- TPS
- 실패율
- CPU, 메모리
- slow query 목록
- 각 느린 SQL의 `EXPLAIN ANALYZE`

추천 부하 단계:

1. 10 VU, 3분
2. 30 VU, 5분
3. 50 VU, 5분
4. 100 VU, 5분

갑자기 큰 부하를 주기보다 계단식으로 올려야 어느 구간부터 무너지는지 보기가 쉽다.

## 7.7 6단계. 느린 SQL 추출

부하 테스트 후 아래 질문으로 정리한다.

- 가장 많이 호출된 API는 무엇인가
- 가장 느린 API는 무엇인가
- DB 시간이 대부분을 차지하는가
- 어떤 SQL이 full scan 또는 filesort를 유발하는가

예시 확인:

```sql
select
    digest_text,
    count_star,
    avg_timer_wait,
    sum_rows_examined,
    sum_rows_sent
from performance_schema.events_statements_summary_by_digest
order by avg_timer_wait desc
limit 20;
```

그리고 느린 쿼리에 대해 개별적으로:

```sql
explain analyze
select p.*
from post p
join member m on p.user_id = m.id
order by p.created_date desc;
```

이때 보는 포인트:

- `type`
- `rows`
- `filtered`
- `Using filesort`
- `Using temporary`
- 실제 소요 시간

## 7.8 7단계. 인덱스 후보를 하나씩 적용

중요 규칙:

- 한 번에 여러 개 넣지 않는다
- 인덱스는 하나씩 넣고 다시 테스트한다

이유:

- 어떤 인덱스가 실제로 효과 있었는지 분리해서 봐야 한다
- 여러 개를 한꺼번에 넣으면 원인 추적이 어려워진다

적용 순서 추천:

1. `post(created_date)`
2. `item(category_id, deleted, id)`
3. `comment(post_id, created_date)`
4. 필요 시 `orders(user_id, order_date)`

## 7.9 8단계. Flyway로 반영

이 프로젝트는 Flyway를 사용한다.  
따라서 인덱스 추가도 수동 SQL이 아니라 마이그레이션으로 관리해야 한다.

예시:

`src/main/resources/db/migration/V5__add_hot_query_indexes.sql`

예시 내용:

```sql
create index idx_post_created_date on post(created_date);
create index idx_item_category_deleted_id on item(category_id, deleted, id);
```

원칙:

- 인덱스 이름을 명확하게 짓기
- 왜 넣는지 문서와 커밋 메시지에 남기기
- 한 버전에 너무 많은 인덱스를 몰아넣지 않기

## 7.10 9단계. 재테스트

같은 조건에서 다시 `k6`를 돌린다.

비교 포인트:

- P95가 실제로 내려갔는가
- TPS가 올랐는가
- slow query가 사라졌는가
- 쓰기 API가 오히려 느려지지 않았는가

채택 기준 예시:

- P95 20% 이상 개선
- rows examined 유의미하게 감소
- 실행계획에서 filesort/temp 제거
- 쓰기 성능 악화가 허용 범위 내

폐기 기준 예시:

- 개선이 거의 없음
- 인덱스만 늘고 저장 성능이 악화
- 메모리 사용량만 증가

## 7.11 10단계. 운영 반영 체크

운영 반영 전 최종 확인:

- 마이그레이션이 dev/staging/prod 모두에서 안전한가
- 인덱스 생성 시간이 허용 가능한가
- 운영 중 DDL 락 이슈가 없는가
- 롤백 방안이 있는가

가능하면:

- staging에서 먼저 적용
- 같은 `k6` 시나리오 재실행
- 문제 없으면 운영 반영

## 8. 현재 프로젝트에 대한 우선순위 결론

지금 코드 기준으로 가장 먼저 볼 것은 아래 두 가지다.

### 8.1 게시글 목록 정렬

이유:

- `/api/posts?sort=desc`는 커뮤니티 메인에서 자주 조회될 가능성이 높다
- 정렬 컬럼 인덱스가 보이지 않는다
- 데이터가 커질수록 체감 성능 차이가 크게 날 수 있다

첫 후보:

```sql
create index idx_post_created_date on post(created_date);
```

### 8.2 카테고리 상품/대표 이미지/카운트

이유:

- 카테고리 카드, 상품 목록, 대표 이미지 쿼리가 모두 `item` 테이블에 의존한다
- `deleted = false`와 `category_id`가 함께 계속 쓰인다
- 집계와 대표 이미지 조회는 데이터가 늘면 금방 무거워질 수 있다

첫 후보:

```sql
create index idx_item_category_deleted_id on item(category_id, deleted, id);
```

## 9. 반대로 지금 당장 안 해도 되는 것

### 9.1 `member.email` 인덱스 추가

이미 유니크 제약으로 인덱스가 있다.  
로그인 병목이 있다면 DB 인덱스보다 다른 원인을 먼저 봐야 한다.

### 9.2 `item.deleted` 단독 인덱스

낮은 선택도 때문에 우선순위가 낮다.

### 9.3 `post.content` 인덱스

현재는 검색 기능이 없으므로 필요 없다.

### 9.4 H2에서 인덱스 효과 검증

의미가 약하다. 반드시 MySQL에서 본다.

## 10. 추천 실행 순서 한 줄 버전

1. MySQL 기반 성능 테스트 환경을 만든다
2. 데이터 양을 충분히 늘린다
3. `k6`로 베이스라인을 잰다
4. slow query와 `EXPLAIN ANALYZE`로 느린 SQL을 찾는다
5. 인덱스를 하나씩 Flyway로 추가한다
6. 같은 시나리오로 재테스트한다
7. 개선 효과가 확인된 인덱스만 남긴다

## 11. 바로 실행할 첫 스프린트 제안

이번 프로젝트에서 가장 현실적인 1차 작업은 아래다.

### 작업 1. 성능 테스트 전용 환경 마련

- MySQL 컨테이너 추가
- Redis 포함 실행
- 부하 테스트용 프로필 분리

### 작업 2. 테스트 데이터 증량

- 상품, 게시글, 댓글, 주문 데이터 대량 생성 스크립트 마련
- 기존 `scripts/seed_real_products.py`, `scripts/seed_community_activity.py` 확장

### 작업 3. `k6` 스크립트 작성

- 공개 조회 시나리오
- 로그인 장바구니/주문 시나리오
- 게시글/댓글 시나리오

### 작업 4. 첫 인덱스 검증

- `post(created_date)`
- `item(category_id, deleted, id)`

### 작업 5. 결과 문서화

- 인덱스 전/후 P95 비교
- `EXPLAIN ANALYZE` 비교
- 채택/보류 결정

## 12. 최종 결론

현재 `shopping` 프로젝트에서는 "인덱스부터 먼저 추가"보다 아래 순서가 맞다.

`MySQL 환경 준비 -> 데이터 증량 -> k6 부하 테스트 -> 느린 SQL 추출 -> EXPLAIN ANALYZE -> 인덱스 1개씩 적용 -> Flyway 반영 -> 재검증`

그리고 지금 기준으로 가장 가능성이 높은 첫 후보는 아래 두 개다.

- `post(created_date)`
- `item(category_id, deleted, id)`

반면 아래는 지금 당장 우선순위가 낮다.

- `member.active`
- `item.deleted` 단독 인덱스
- `orders.status` 단독 인덱스
- `post.content` 일반 인덱스

즉, 이 프로젝트에서 인덱스는 "값 종류가 적은 상태값"보다 "정렬 컬럼, 조인 컬럼, 카테고리 조건, 실제 핫쿼리" 중심으로 접근하는 것이 맞다.
