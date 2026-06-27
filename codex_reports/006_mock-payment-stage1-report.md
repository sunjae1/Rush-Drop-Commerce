# 006 Mock Payment Stage 1 Report

작성일: 2026-06-26

## 목표

Toss 같은 실제 PG를 붙이기 전에, 쇼핑몰 내부에서 먼저 결제 도메인의 기본 뼈대를 세웠다.

이번 1단계의 목표는 다음이다.

- `Payment` 데이터 모델 추가
- 주문 상태를 결제 흐름에 맞게 확장
- 장바구니 주문을 `결제 준비 -> Mock 승인` 흐름으로 분리
- 결제 금액 검증과 재고 예약/복구 테스트
- 프론트 장바구니 결제 버튼을 Mock 결제 흐름에 연결

## 구현 전

기존 흐름은 장바구니에서 바로 `POST /api/orders`를 호출했다.

결과:

- 주문 생성과 결제 완료가 한 번에 끝남
- 주문 상태가 바로 `PAID`
- 결제 테이블 없음
- 결제 대기, 결제 실패, 금액 검증, PG 승인 응답을 표현하기 어려움

## 구현 후

새 Mock 결제 흐름은 두 단계다.

1. `POST /api/payments/mock/prepare`
   - 장바구니를 주문으로 변환
   - 주문 상태를 `PAYMENT_PENDING`으로 저장
   - 상품 재고를 `FOR UPDATE` 조회 후 예약 차감
   - `Payment`를 `READY` 상태로 생성
   - 장바구니를 비움

2. `POST /api/payments/mock/confirm`
   - `paymentOrderId`와 `amount`를 받음
   - DB의 결제 금액과 요청 금액을 비교
   - 금액이 맞으면 결제 `APPROVED`, 주문 `PAID`
   - 금액이 다르면 결제 `FAILED`, 주문 `PAYMENT_FAILED`, 예약 재고 복구

## 주요 변경 파일

백엔드:

- `shopping_be/src/main/java/myex/shopping/domain/Payment.java`
- `shopping_be/src/main/java/myex/shopping/domain/PaymentProvider.java`
- `shopping_be/src/main/java/myex/shopping/domain/PaymentStatus.java`
- `shopping_be/src/main/java/myex/shopping/domain/OrderStatus.java`
- `shopping_be/src/main/java/myex/shopping/domain/Order.java`
- `shopping_be/src/main/java/myex/shopping/service/PaymentService.java`
- `shopping_be/src/main/java/myex/shopping/controller/api/ApiPaymentController.java`
- `shopping_be/src/main/java/myex/shopping/repository/PaymentRepository.java`
- `shopping_be/src/main/java/myex/shopping/repository/jpa/JpaPaymentRepository.java`
- `shopping_be/src/main/java/myex/shopping/dto/paymentdto/PaymentDto.java`
- `shopping_be/src/main/java/myex/shopping/dto/paymentdto/PaymentConfirmRequest.java`
- `shopping_be/src/main/java/myex/shopping/exception/PaymentException.java`
- `shopping_be/src/main/resources/db/migration/V8__create_payment_table.sql`
- `shopping_be/src/test/java/myex/shopping/controller/api/ApiPaymentControllerTest.java`

프론트:

- `shopping_fe/src/api/types.ts`
- `shopping_fe/src/api/client.ts`
- `shopping_fe/src/api/demoClient.ts`
- `shopping_fe/src/contexts/CartContext.tsx`
- `shopping_fe/src/pages/CartPage.tsx`
- `shopping_fe/src/test/cart-page.test.tsx`
- `shopping_fe/src/test/api-client-auth.test.ts`
- `shopping_fe/e2e/commerce.spec.ts`
- `shopping_fe/e2e/fixtures/mockApi.ts`

## 데이터 모델

새 `payment` 테이블:

- `id`
- `order_id`
- `provider`
- `status`
- `payment_order_id`
- `provider_payment_key`
- `amount`
- `requested_at`
- `approved_at`
- `failure_reason`

주문 상태:

- `ORDERED`
- `PAYMENT_PENDING`
- `PAID`
- `PAYMENT_FAILED`
- `CANCELLED`

`orders.status`는 기존 MySQL `ENUM`에서 `VARCHAR(30)`으로 변경했다. 새 결제 상태가 늘어날 때마다 DB enum을 계속 수정하는 부담을 줄이기 위해서다.

## API 계약

결제 준비:

```http
POST /api/payments/mock/prepare
```

응답 예시:

```json
{
  "id": 1,
  "orderId": 10,
  "paymentOrderId": "mock_...",
  "provider": "MOCK",
  "status": "READY",
  "amount": 30000,
  "requestedAt": "2026-06-26T..."
}
```

결제 승인:

```http
POST /api/payments/mock/confirm
```

요청 예시:

```json
{
  "paymentOrderId": "mock_...",
  "amount": 30000
}
```

성공 시:

- `Payment.status = APPROVED`
- `Order.status = PAID`

금액 불일치 시:

- HTTP `400`
- `code = PAYMENT_AMOUNT_MISMATCH`
- `Payment.status = FAILED`
- `Order.status = PAYMENT_FAILED`
- 예약 재고 복구

## 테스트 결과

백엔드:

- `.\gradlew.bat test --tests myex.shopping.controller.api.ApiPaymentControllerTest`
  - 성공
  - Mock 결제 준비/승인 검증
  - 금액 불일치 실패 처리와 재고 복구 검증

- `.\gradlew.bat test --tests myex.shopping.service.OrderServiceTest --tests myex.shopping.controller.api.ApiOrderControllerTest`
  - 성공
  - 기존 주문 서비스와 기존 주문 API 회귀 확인

- `.\gradlew.bat test`
  - 성공

프론트:

- `npm test -- src/test/cart-page.test.tsx src/test/api-client-auth.test.ts`
  - 성공

- `npm run build`
  - 성공

- `npm test`
  - 성공
  - 14개 테스트 파일, 44개 테스트 통과

E2E:

- `npx playwright test e2e/commerce.spec.ts`
  - 성공
  - 상품 담기, Mock 결제, 마이페이지 주문 확인, 프로필 수정, 로그아웃 흐름 통과

Flyway/dev DB:

- 앱 컨테이너 재시작 후 Flyway 로그에서 `Successfully validated 8 migrations` 확인
- 현재 스키마 버전 `8`
- `payment` 테이블 생성 확인
- `orders.status`가 `varchar(30)`으로 변경된 것 확인

## 의미 있는 포인트

이번 구현은 단순히 버튼 문구를 바꾼 것이 아니라, 결제 도메인을 분리했다는 점이 핵심이다.

- 결제 준비와 결제 승인을 API로 분리했다.
- 결제 금액을 프론트 값 그대로 믿지 않고 DB의 결제 금액과 비교한다.
- 결제 준비 단계에서 재고를 예약한다.
- 재고 조회 순서를 상품 ID 기준으로 고정하고 `findByIdForUpdate`를 사용해 동시성 충돌 가능성을 낮췄다.
- 승인 성공/실패에 따라 주문 상태와 결제 상태가 따로 전이된다.
- 실제 Toss 연동 시 `PaymentService`의 Mock 승인 부분을 Toss confirm 호출로 바꿔 확장할 수 있다.

## 남은 한계

아직 Mock 단계라서 실제 PG 호출은 없다.

남은 작업:

- Toss 테스트 키 기반 결제창/결제위젯 연동
- Toss 승인 API 호출용 `TossPaymentGatewayClient`
- 결제 실패 후 장바구니 복구 정책
- 결제 대기 만료 처리
- 결제 취소/환불 API
- 웹훅 수신 API
- 운영용 결제 로그/감사 로그

현재는 결제 실패 시 재고는 복구하지만 장바구니는 복구하지 않는다. 실제 서비스라면 실패한 주문을 다시 결제하게 할지, 장바구니를 복원할지 정책을 정해야 한다.

## 결론

1단계 Mock 결제는 구현 완료다.

이제 프로젝트는 기존 CRUD 쇼핑몰에서 한 단계 올라가서, 결제 도메인의 핵심인 주문 상태 전이, 결제 상태 저장, 금액 검증, 재고 예약/복구를 설명할 수 있는 구조가 됐다.
