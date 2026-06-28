# 007 Toss Test Payment Stage 2 Report

작성일: 2026-06-28

## 목표

Mock 결제 다음 단계로 Toss Payments 테스트 결제창을 연결했다.

이번 단계의 목표:

- 프론트에서 Toss V2 SDK 결제창 열기
- 백엔드에서 Toss 결제 승인 API 호출
- 클라이언트 금액을 그대로 믿지 않고 서버 저장 금액으로 검증
- Toss 성공/실패 redirect 처리
- 기존 Mock 결제 테스트 흐름 유지

## Toss 공식 문서 기준

Toss Payments V2 결제창 흐름은 다음 원칙을 따른다.

- 브라우저는 클라이언트 키로 결제창 인증만 요청한다.
- 백엔드는 시크릿 키로 `/v1/payments/confirm`을 호출해 결제를 최종 승인한다.
- `successUrl`의 `amount`는 반드시 서버에 저장된 결제 금액과 비교한다.
- 시크릿 키는 `Basic base64(secretKey:)` 형식으로 보낸다. 끝의 `:`가 필요하다.

참고한 문서:

- Toss Payments V2 `카드/간편결제 통합결제창 연동하기`
- Toss Payments V2 LLM Quick Reference

## 환경변수

프론트엔드:

```env
VITE_TOSS_CLIENT_KEY=test_ck_...
```

백엔드:

```env
TOSS_SECRET_KEY=test_sk_...
TOSS_API_BASE_URL=https://api.tosspayments.com
```

주의:

- `VITE_TOSS_CLIENT_KEY`는 브라우저에 노출되어도 되는 클라이언트 키다.
- `TOSS_SECRET_KEY`는 백엔드에만 둔다.
- `TOSS_SECRET_KEY`를 프론트 `.env`나 GitHub에 올리면 안 된다.

## 백엔드 구현

추가 API:

```http
POST /api/payments/toss/prepare
```

역할:

- 장바구니를 주문으로 전환
- 주문 상태를 `PAYMENT_PENDING`으로 저장
- 재고 예약 차감
- `Payment.provider = TOSS`
- Toss 결제창에서 사용할 `paymentOrderId` 생성

```http
POST /api/payments/toss/confirm
```

역할:

- Toss success redirect의 `paymentKey`, `orderId`, `amount`를 받음
- DB에 저장된 결제 금액과 요청 금액 비교
- 금액이 맞으면 Toss confirm API 호출
- 승인 성공 시 `providerPaymentKey = Toss paymentKey`
- 주문 상태를 `PAID`로 변경

```http
POST /api/payments/toss/fail
```

역할:

- Toss 결제창 실패/취소 결과를 실패 처리
- 주문 상태를 `PAYMENT_FAILED`로 변경
- 예약 재고 복구

주요 파일:

- `shopping_be/src/main/java/myex/shopping/payment/TossPaymentClient.java`
- `shopping_be/src/main/java/myex/shopping/payment/TossPaymentGateway.java`
- `shopping_be/src/main/java/myex/shopping/payment/TossPaymentConfirmation.java`
- `shopping_be/src/main/java/myex/shopping/dto/paymentdto/TossPaymentConfirmRequest.java`
- `shopping_be/src/main/java/myex/shopping/service/PaymentService.java`
- `shopping_be/src/main/java/myex/shopping/controller/api/ApiPaymentController.java`

## 프론트 구현

추가 패키지:

```bash
npm install @tosspayments/tosspayments-sdk
```

추가 화면:

- `/checkout/toss/success`
- `/checkout/toss/fail`

결제 페이지 변경:

- `VITE_TOSS_CLIENT_KEY`가 있으면 Toss 테스트 카드가 기본 선택된다.
- 키가 비어 있거나 데모 모드면 Mock 카드가 기본 선택된다.
- Toss 결제창을 열기 전 백엔드 `toss/prepare`를 호출한다.
- Toss redirect 이후 주문 스냅샷을 복원하기 위해 `sessionStorage`를 사용한다.

주요 파일:

- `shopping_fe/src/pages/CheckoutPage.tsx`
- `shopping_fe/src/pages/TossPaymentSuccessPage.tsx`
- `shopping_fe/src/pages/TossPaymentFailPage.tsx`
- `shopping_fe/src/lib/checkoutSnapshot.ts`
- `shopping_fe/src/api/client.ts`
- `shopping_fe/src/app/App.tsx`
- `shopping_fe/src/vite-env.d.ts`

## 결제 흐름

```text
장바구니
-> /checkout
-> POST /api/payments/toss/prepare
-> Toss SDK requestPayment()
-> Toss 결제창
-> /checkout/toss/success?paymentKey=...&orderId=...&amount=...
-> POST /api/payments/toss/confirm
-> /checkout/complete
```

실패 흐름:

```text
Toss 결제창 실패 또는 취소
-> /checkout/toss/fail?code=...&message=...&orderId=...
-> POST /api/payments/toss/fail
-> 예약 재고 복구
-> /checkout/complete
```

## 테스트 결과

백엔드:

```bash
.\gradlew.bat test --tests myex.shopping.controller.api.ApiPaymentControllerTest
```

성공.

검증:

- Toss 결제 준비
- Toss confirm gateway 호출
- Toss `paymentKey` 저장
- 금액 불일치 시 Toss confirm API 미호출
- 금액 불일치 시 결제 실패 처리와 재고 복구

```bash
.\gradlew.bat test
```

성공.

프론트:

```bash
npm test -- src/test/checkout-page.test.tsx src/test/cart-page.test.tsx src/test/api-client-auth.test.ts
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

성공. 기존 Mock 결제 기반 commerce flow가 유지되는 것 확인.

## 남은 작업

- 실제 Toss 테스트 키 입력 후 브라우저에서 결제창 수동 검증
- Toss 결제 취소/환불 API
- Toss webhook 수신 후 결제 재조회
- 결제 실패 후 장바구니 복구 정책
- 결제 대기 주문 만료 처리
- 운영 배포 환경의 callback URL 점검

## 실행 체크리스트

1. `shopping_fe/.env.development` 또는 배포 환경에 `VITE_TOSS_CLIENT_KEY` 입력
2. `shopping_be/.env` 또는 배포 환경에 `TOSS_SECRET_KEY` 입력
3. 백엔드 재시작
4. 프론트 dev 서버 재시작
5. 장바구니에 상품 담기
6. `/checkout`에서 Toss 테스트 카드 선택
7. `Toss 결제창 열기` 클릭
