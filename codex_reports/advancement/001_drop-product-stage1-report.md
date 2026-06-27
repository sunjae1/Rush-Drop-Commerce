# 001 Drop Product Stage 1 Report

작성일: 2026-06-25

## 목표

프로젝트 방향을 `선착순 한정 수량 드롭 쇼핑몰`로 잡고, 1단계로 기존 상품을 드롭 상품으로 등록/조회/수정할 수 있게 만든다.

이번 단계의 범위는 결제, 대기열, 재고 동시성 처리까지가 아니라 드롭 상품 메타데이터와 화면 노출을 고정하는 것이다.

## 구현 내용

### 백엔드

- `Item`에 드롭 판매 필드 추가
  - `dropProduct`
  - `dropStartsAt`
  - `dropEndsAt`
  - `dropPurchaseLimit`
- 드롭 상태 enum 추가
  - `STANDARD`
  - `UPCOMING`
  - `LIVE`
  - `ENDED`
- `Item.resolveDropSaleStatus(LocalDateTime now)`로 현재 상태 계산 추가
- 상품 등록/수정 form에 드롭 필드 추가
- `POST /api/items`, `PUT /api/items/{id}`에서 드롭 필드 검증 추가
  - 드롭 상품이면 시작 시간 필수
  - 드롭 상품이면 종료 시간 필수
  - 종료 시간은 시작 시간 이후여야 함
  - 1인 구매 제한은 1 이상이어야 함
- `ItemDto`, `ItemDtoDetail`, `ItemEditDto`에 드롭 필드와 상태 노출
- JPA/Memory item repository 업데이트 로직에 드롭 필드 반영
- Flyway migration 추가
  - `V5__add_drop_sale_fields_to_item.sql`

### 프론트엔드

- `Item`, `ItemMutationInput` 타입에 드롭 필드 추가
- API client normalize/form-data 변환에 드롭 필드 반영
- demo seed에 드롭 예정 상품 추가
- demo client에서 드롭 상태 계산 추가
- 관리자 상품 등록/수정 화면에 드롭 상품 설정 추가
  - 드롭 상품 체크박스
  - 드롭 시작/종료 datetime 입력
  - 1인 구매 제한 입력
- 상품 카드에 드롭 상태 배지 노출
- 상품 상세에 드롭 상태, 일정, 1인 제한 표시
- 상세 화면에서 드롭 예정/종료 상품은 장바구니 버튼을 비활성화

## 검증 결과

### 백엔드

명령:

```bash
./gradlew.bat test
```

결과:

- 성공
- 전체 Gradle 테스트 통과

추가된 주요 검증:

- 일반 상품은 `dropProduct=false`, `dropSaleStatus=STANDARD`로 반환
- 드롭 상품 등록 시 `dropProduct=true`, `dropPurchaseLimit`, `dropSaleStatus=LIVE` 반환
- 드롭 종료 시간이 시작 시간보다 빠르면 `400 Bad Request`
- 드롭 상품을 일반 상품으로 수정하면 드롭 메타데이터 제거

### 프론트엔드

명령:

```bash
npm run build
```

결과:

- 성공
- TypeScript type check + Vite production build 통과

명령:

```bash
npm test
```

결과:

- 성공
- 13개 test file 통과
- 40개 test 통과

참고:

- React Router v7 future flag 경고는 기존 테스트 경고이며 실패 원인은 아님

### 미완료 검증

`git diff --check`는 실행했지만 현재 작업 루트가 Git 저장소로 인식되지 않아 사용할 수 없었다.

## 하네스 엔지니어링 확인

현재 프로젝트에는 테스트 하네스가 부분적으로 잘 구성되어 있다.

이미 있는 것:

- 백엔드
  - Gradle/JUnit 테스트
  - `SpringBootTest`
  - `MockMvc`
  - JPA repository 테스트
  - H2 test profile
  - Redis Testcontainers 기반 테스트 베이스
  - k6 성능 테스트 스크립트
- 프론트엔드
  - Vitest
  - React Testing Library
  - API client 테스트
  - Playwright E2E
  - Playwright visual regression
  - E2E mock API fixture

부족한 것:

- 현재 작업 루트에서 `.github/workflows`는 확인되지 않았다.
- 드롭 상품 전용 race condition 테스트 하네스는 아직 없다.
- 결제 성공/실패/만료 callback을 검증하는 payment harness는 아직 없다.
- 실 DB 수준의 재고 동시성 검증은 아직 없다.
- 드롭 예정/종료 상품의 구매 차단은 현재 프론트 상세 화면에서만 막고 있으며, 서버의 장바구니/주문 API 레벨 enforcement는 다음 단계에서 추가해야 한다.

## 하네스 추가 방법

이번 작업에서는 하네스를 바로 구현하지 않았다. 다음 단계에서 아래 순서로 추가하는 것을 권장한다.

1. 드롭 구매 동시성 테스트 하네스
   - MySQL Testcontainers 기반 테스트 추가
   - H2가 아니라 MySQL로 실행해야 lock/transaction 동작을 더 정확히 볼 수 있음
   - `ExecutorService` 또는 `CompletableFuture`로 N개 동시 구매 요청 발생
   - 검증 조건: 성공 주문 수가 드롭 재고를 초과하지 않아야 함

2. 서버 구매 차단 테스트
   - 드롭 예정 상품 장바구니 담기 실패
   - 드롭 종료 상품 장바구니 담기 실패
   - 드롭 진행 상품 장바구니 담기 성공
   - 1인 구매 제한 초과 실패

3. 결제 하네스
   - 실제 PG 연동 전 Mock payment adapter 추가
   - 결제 성공, 실패, timeout, 중복 callback 시나리오 분리
   - idempotency key 테스트 추가
   - 결제 실패/만료 시 재고 예약 복구 테스트 추가

4. 프론트 E2E 하네스
   - Playwright mock API에 드롭 상태 필드 추가
   - 관리자 드롭 상품 생성 플로우 E2E 추가
   - 상품 상세에서 `UPCOMING`, `LIVE`, `ENDED` 상태별 버튼/문구 검증

5. CI 하네스
   - `.github/workflows/ci.yml` 추가
   - 백엔드: `./gradlew.bat test` 또는 Linux runner 기준 `./gradlew test`
   - 프론트: `npm ci`, `npm run build`, `npm test`
   - 선택: Playwright chromium E2E
   - 선택: k6 smoke scenario

## 다음 단계 제안

다음 구현 우선순위는 서버 API 레벨 구매 차단이다.

1. `CartService.addItem`에서 드롭 상태 검사
2. 드롭 예정/종료 상품 차단 exception 추가
3. 1인 구매 제한 검사
4. API 테스트 추가
5. 이후 재고 예약, 결제 대기, 동시성 테스트로 확장
