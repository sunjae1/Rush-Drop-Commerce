# DB 인덱스 성능 테스트 결과 보고서

## 개요

- 대상 프로젝트: `shopping_be`
- 목적: DB/인덱스 관점에서 조회 성능 병목 확인 및 인덱스 효과 검증
- 테스트 방식:
  - Docker 기반 perf 환경 실행
  - MySQL 컨테이너 DB 사용
  - Python DB 직접 시드
  - `k6`로 읽기 부하 테스트
  - MySQL `slow_log`와 `EXPLAIN ANALYZE`로 병목 확인

## 테스트 환경

- 앱: Spring Boot `perf` 프로필
- DB: MySQL 8.0 컨테이너
- Redis: Redis 컨테이너
- 부하 도구: `k6`
- 시드 방식: Python + PyMySQL 직접 insert

관련 파일:

- [docker-compose.perf.yml](C:/Users/kimsunjae/Desktop/NewFolder/Java_INTELLIJ/MyProject/shopping/shopping_be/docker-compose.perf.yml)
- [application-perf.yml](C:/Users/kimsunjae/Desktop/NewFolder/Java_INTELLIJ/MyProject/shopping/shopping_be/src/main/resources/application-perf.yml)
- [seed_perf_db.py](C:/Users/kimsunjae/Desktop/NewFolder/Java_INTELLIJ/MyProject/shopping/scripts/seed_perf_db.py)
- [catalog-read.js](C:/Users/kimsunjae/Desktop/NewFolder/Java_INTELLIJ/MyProject/shopping/shopping_be/perf/k6/catalog-read.js)

## 시드 데이터

- category: 20
- member: 1000
- item: 5000
- post: 10000
- comment: 30000
- orders: 10000
- order_item: 30000
- cart_item: 5000

샘플 조회 ID:

- category_id: 6
- item_id: 10
- post_id: 7

## 테스트 시나리오

`k6` 시나리오에서 아래 API를 반복 조회했다.

- `GET /actuator/health`
- `GET /api/categories`
- `GET /api/items`
- `GET /api/items?categoryId=6`
- `GET /api/posts?sort=desc`
- `GET /api/items/10`
- `GET /api/posts/7`

## 1차 병목 분석

`slow_log`와 `EXPLAIN ANALYZE` 기준으로 가장 무거운 쿼리는 카테고리 대표 이미지 조회였다.

대상 쿼리:

```sql
select i1_0.category_id, i1_0.image_url
from item i1_0
where i1_0.deleted = 0
  and i1_0.category_id is not null
  and i1_0.id = (
    select min(i2_0.id)
    from item i2_0
    where i2_0.deleted = 0
      and i2_0.category_id = i1_0.category_id
  )
```

관찰 내용:

- `rows_examined`: 약 `1,255,010`
- 기존 실행시간: 대략 `0.7s ~ 1.8s`
- 원인:
  - `item` 바깥 스캔
  - 각 row마다 dependent subquery 반복
  - `category_id` 인덱스만으로는 `deleted + min(id)` 패턴에 부족

관련 코드:

- [JpaCategoryRepository.java](C:/Users/kimsunjae/Desktop/NewFolder/Java_INTELLIJ/MyProject/shopping/shopping_be/src/main/java/myex/shopping/repository/jpa/JpaCategoryRepository.java)

## 적용 인덱스

적용 SQL:

```sql
CREATE INDEX idx_item_category_deleted_id
ON item(category_id, deleted, id);
```

의도:

- `category_id = ?`
- `deleted = 0`
- 그 조건 안에서 `min(id)`

이 패턴에 맞춘 복합 인덱스 추가

## 실행계획 변화

인덱스 적용 전:

- `fk_item_category` 사용
- 이후 `deleted = 0` 별도 필터
- dependent subquery 반복 비용 큼

인덱스 적용 후:

- `Covering index lookup on i2 using idx_item_category_deleted_id`
- `category_id, deleted`를 인덱스에서 함께 처리
- 서브쿼리 1회당 비용 감소
- 최상위 완료 시점 기준 대략 `0.77s -> 0.22s`

## k6 결과 비교

### 인덱스 적용 전

- 평균 응답시간: `602.98ms`
- `p95`: `4.88s`
- 처리량: `21.33 req/s`
- 실패율: `0%`

### 인덱스 적용 후 1차

- 평균 응답시간: `125.08ms`
- `p95`: `514ms`
- 처리량: `60.11 req/s`
- 실패율: `0%`

### 인덱스 적용 후 재측정

- 평균 응답시간: `103.14ms`
- `p95`: `391.81ms`
- 처리량: `65.95 req/s`
- 실패율: `0%`

## 결론

- `idx_item_category_deleted_id` 인덱스는 실제 효과가 확인되었다.
- 대표 이미지 조회 쿼리 병목은 완화되었다.
- `k6` 기준 응답시간과 처리량 모두 유의미하게 개선되었다.
- 재측정에서도 `p95 < 500ms` 기준을 통과했다.

현재 시점 결론:

- 이 인덱스는 채택 가치가 높다.
- 다음 단계는 이 인덱스를 Flyway 마이그레이션으로 정식 반영하는 것이다.

## 권장 후속 작업

1. `idx_item_category_deleted_id`를 Flyway SQL로 반영
2. 동일 방식으로 다음 느린 쿼리 후보 탐색
3. 필요하면 `/api/categories` 쿼리 자체 리팩터링 검토
4. 시드 규모를 더 올려 재검증

## 비고

- 이번 테스트는 DB/인덱스 성능에 집중했다.
- 이미지 업로드/S3 병목은 이번 범위에 포함하지 않았다.
- 상품 이미지 값은 기존 DB의 S3 key를 재사용하는 방식으로 가정했다.
