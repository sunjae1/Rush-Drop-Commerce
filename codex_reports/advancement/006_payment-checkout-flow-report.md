# 006 Payment Checkout Flow Report

작성일: 2026-06-27

## 질문 정리

`providerPaymentKey`는 Toss 같은 PG사가 결제 승인 후 발급하는 PG 쪽 결제 식별자다.

- `paymentOrderId`: 우리 서비스가 만든 결제 주문번호
- `providerPaymentKey`: PG사가 승인 후 내려주는 결제키
- 실제 결제 취소, 환불, 결제 조회는 보통 `providerPaymentKey`를 들고 PG API를 다시 호출한다.
- 현재 Mock 결제에서는 진짜 PG가 없기 때문에 `mock_...` 형태의 가짜 값을 저장한다.

## 구현 전

기존 프론트 흐름은 장바구니에서 버튼을 누르면 바로 Mock 결제가 승인됐다.

```text
장바구니
-> POST /api/payments/mock/prepare
-> POST /api/payments/mock/confirm
-> 승인 메시지 표시
```

문제:

- 사용자 입장에서는 기존 "주문하기"와 거의 같아 보였다.
- 주문 금액, 배송지, 결제수단을 확인하는 결제 화면이 없었다.
- 성공/실패/금액 불일치 같은 결제 시나리오를 화면에서 보여주기 어려웠다.

## 구현 후

새 흐름:

```text
장바구니
-> 주문/결제 페이지
-> 주문 상품, 배송지, 결제수단, 총 결제 금액 확인
-> Mock 카드 결제
-> 승인 성공 / 실패 처리 / 금액 불일치 테스트
-> 결제 결과 페이지
```

추가 화면:

- `/checkout`
- `/checkout/complete`

장바구니 버튼은 즉시 결제를 실행하지 않고 `/checkout`으로 이동한다.

## 백엔드 변경

추가 API:

```http
POST /api/payments/mock/fail
```

역할:

- Mock 결제를 명시적으로 실패 처리한다.
- `Payment.status = FAILED`
- `Order.status = PAYMENT_FAILED`
- 결제 준비 단계에서 예약 차감했던 재고를 복구한다.

추가 파일:

- `shopping_be/src/main/java/myex/shopping/dto/paymentdto/PaymentFailRequest.java`

변경 파일:

- `shopping_be/src/main/java/myex/shopping/controller/api/ApiPaymentController.java`
- `shopping_be/src/main/java/myex/shopping/service/PaymentService.java`
- `shopping_be/src/test/java/myex/shopping/controller/api/ApiPaymentControllerTest.java`

## 프론트 변경

추가 파일:

- `shopping_fe/src/pages/CheckoutPage.tsx`
- `shopping_fe/src/pages/CheckoutCompletePage.tsx`
- `shopping_fe/src/test/checkout-page.test.tsx`

변경 파일:

- `shopping_fe/src/app/App.tsx`
- `shopping_fe/src/pages/CartPage.tsx`
- `shopping_fe/src/api/client.ts`
- `shopping_fe/src/api/demoClient.ts`
- `shopping_fe/src/styles/app.css`
- `shopping_fe/e2e/commerce.spec.ts`
- `shopping_fe/e2e/fixtures/mockApi.ts`
- `shopping_fe/src/test/cart-page.test.tsx`

프론트 API 클라이언트는 결제 단계를 분리했다.

- `prepareMockPayment()`
- `confirmMockPayment(paymentOrderId, amount)`
- `failMockPayment(paymentOrderId, reason)`
- 기존 `checkout()`은 호환용으로 남겨두고 내부에서 prepare/confirm을 호출한다.

## 화면에서 보여지는 차이

이전:

- 장바구니에서 버튼 클릭
- 바로 "Mock 결제가 승인되었습니다" 표시

이후:

- 장바구니에서 주문/결제 페이지로 이동
- 주문 상품 목록 확인
- 배송지 입력 확인
- 결제수단 선택
- 총 결제 금액 확인
- Mock 카드 결제 버튼 실행
- 승인 성공/실패/금액 불일치 테스트 가능
- 완료 페이지에서 결제 상태, 주문 번호, 우리 결제 주문번호, PG 결제키 확인

## 테스트 결과

백엔드:

```bash
.\gradlew.bat test --tests myex.shopping.controller.api.ApiPaymentControllerTest
```

성공.

검증 내용:

- Mock 결제 준비 후 승인
- 금액 불일치 실패 처리
- Mock 실패 API로 실패 처리
- 실패 시 주문 상태 변경과 재고 복구

```bash
.\gradlew.bat test
```

성공.

프론트:

```bash
npm test -- src/test/cart-page.test.tsx src/test/checkout-page.test.tsx src/test/api-client-auth.test.ts
```

성공. 3개 파일, 12개 테스트 통과.

```bash
npm run build
```

성공.

```bash
npm test
```

성공. 15개 파일, 46개 테스트 통과.

E2E:

```bash
npx playwright test e2e/commerce.spec.ts
```

성공. 장바구니, 결제 페이지, 결제 완료 페이지, 마이페이지 주문 확인 흐름 통과.

## 포트폴리오 repo 방향

현재처럼 백엔드와 프론트가 따로 Git에 올라가 있으면 각각의 기술 스택은 보기 쉽지만, 지금 프로젝트의 강점인 "하나의 서비스 흐름"은 덜 보일 수 있다.

포트폴리오용으로는 통합본 repo를 하나 새로 만드는 쪽을 추천한다.

추천 구조:

```text
shopping-drop-market/
  shopping_be/
  shopping_fe/
  scripts/
  codex_reports/advancement/
  README.md
  start-dev.cmd
```

이유:

- 평가자가 한 repo에서 도메인 기획, 백엔드, 프론트, 테스트, 보고서를 같이 볼 수 있다.
- 결제, 드롭 상품, 상세 페이지처럼 FE/BE가 연결된 기능을 설명하기 좋다.
- `codex_reports/advancement`가 작업 히스토리와 의사결정 기록 역할을 한다.
- 기존 BE/FE 분리 repo는 보관하고, 포트폴리오 대표 링크는 통합본으로 두는 방식이 가장 깔끔하다.

주의:

- `.env`, 실제 AWS 키, Toss secret key, DB 비밀번호는 절대 커밋하지 않는다.
- 공개 repo에는 `.env.example`만 둔다.
- 보고서에 민감한 키나 실제 계정 정보가 들어가 있지 않은지 확인한다.

## 남은 개선 후보

- Toss 테스트 결제창 연결
- 결제 실패 후 장바구니 복구 정책
- 결제 대기 주문 만료 배치
- 결제 취소/환불 API
- Toss webhook 수신 API
- 마이페이지에서 결제 상태와 `providerPaymentKey` 표시
