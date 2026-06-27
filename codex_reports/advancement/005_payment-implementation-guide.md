# 005 Payment Implementation Guide

작성일: 2026-06-26

## 한 줄 결론

처음 결제를 붙인다면 **Toss Payments 결제위젯/결제창 + 백엔드 승인(confirm) 방식**을 추천한다.

이유:

- 한국 쇼핑몰 포트폴리오에 자연스럽다.
- 테스트 키로 실제 카드 결제 없이 흐름을 만들 수 있다.
- 프론트 결제창, 백엔드 승인 API, 주문/재고/동시성 처리를 모두 보여줄 수 있다.
- 지금 프로젝트의 “선착순 한정 수량 드롭 쇼핑몰”과 잘 맞는다.

## 결제는 왜 복잡한가

결제는 버튼 하나가 아니다. 실제로는 아래 흐름이다.

1. 사용자가 장바구니에서 결제하기를 누른다.
2. 백엔드가 주문 초안을 만든다.
3. 백엔드가 금액과 재고를 확정한다.
4. 프론트가 PG사의 결제창을 연다.
5. 사용자가 카드/간편결제로 결제한다.
6. PG사가 성공 정보를 프론트로 돌려준다.
7. 프론트는 그 값을 백엔드로 보낸다.
8. 백엔드는 PG사 서버에 다시 확인 요청을 한다.
9. 진짜 승인된 결제인지 확인되면 주문을 `PAID`로 바꾼다.

중요한 점:

- 프론트가 “성공했어요”라고 말하는 것만 믿으면 안 된다.
- 최종 결제 확정은 백엔드가 PG 서버에 직접 확인해야 한다.
- 금액은 반드시 백엔드 DB의 주문 금액과 비교해야 한다.

## 선택지

### 1. Toss Payments

추천도: 높음

한국 쇼핑몰 포트폴리오라면 가장 자연스럽다. 결제위젯이나 결제창을 붙이고, 백엔드에서 결제 승인 API를 호출하는 방식으로 구현한다.

필요한 키:

- 프론트 공개 키: `VITE_TOSS_CLIENT_KEY`
- 백엔드 비밀 키: `TOSS_SECRET_KEY`
- 위젯 방식이면 별도 위젯 secret을 쓰는 계정/방식도 있으므로 콘솔에서 확인

백엔드 환경변수 예시:

```env
PAYMENT_PROVIDER=TOSS
TOSS_SECRET_KEY=test_...
TOSS_SUCCESS_URL=http://localhost:5173/payments/success
TOSS_FAIL_URL=http://localhost:5173/payments/fail
```

프론트 Vite 환경변수:

```env
VITE_PAYMENT_PROVIDER=TOSS
VITE_TOSS_CLIENT_KEY=test_...
```

장점:

- 한국 사용자에게 익숙한 결제 경험
- 테스트 연동이 비교적 명확함
- 포트폴리오에서 “국내 PG 연동”이라고 설명하기 좋음

단점:

- 문서와 콘솔 용어를 정확히 따라야 함
- 결제 성공 redirect, 실패 redirect, 백엔드 confirm 흐름을 잘 나눠야 함

공식 문서:

- https://docs.tosspayments.com/
- https://docs.tosspayments.com/en/integration-widget

### 2. PortOne

추천도: 중간에서 높음

여러 PG를 한 번에 묶어 쓰는 결제 연동 플랫폼이다. “Toss만 붙이기”보다 추상화가 들어가서 포트폴리오 설명은 좋아질 수 있다.

필요한 키는 PortOne 콘솔의 버전/채널 설정에 따라 달라질 수 있다. 보통 아래 계열을 확인한다.

- 프론트: `VITE_PORTONE_STORE_ID`
- 프론트: `VITE_PORTONE_CHANNEL_KEY`
- 백엔드: `PORTONE_API_SECRET`
- 웹훅 검증을 쓰면: `PORTONE_WEBHOOK_SECRET`

장점:

- 여러 PG로 확장하기 좋음
- 결제 provider를 바꾸는 구조를 보여주기 좋음

단점:

- 처음 구현하는 사람에게는 Toss 단독보다 개념이 하나 더 많음
- “PortOne -> PG사”라는 중간 계층을 이해해야 함

공식 문서:

- https://developers.portone.io/

### 3. Stripe

추천도: 포트폴리오 데모용으로는 가능, 한국 쇼핑몰 도메인에는 낮음

글로벌 결제 예제로는 문서와 테스트 환경이 좋다. 다만 한국 쇼핑몰 느낌을 살리고 싶다면 Toss나 PortOne이 더 자연스럽다.

필요한 키:

- 프론트 공개 키: `VITE_STRIPE_PUBLISHABLE_KEY`
- 백엔드 비밀 키: `STRIPE_SECRET_KEY`
- 웹훅 검증 키: `STRIPE_WEBHOOK_SECRET`

공식 문서:

- https://docs.stripe.com/keys
- https://docs.stripe.com/testing

## 이 프로젝트에 추천하는 선택

1단계는 **Toss Payments 결제창/결제위젯**으로 가는 것을 추천한다.

이 프로젝트는 이미 아래 포트폴리오 포인트가 있다.

- 드롭 오픈 전 장바구니 제한
- 한정 수량
- 재고 동시성
- 주문 생성
- 장바구니

여기에 결제를 붙이면 자연스럽게 아래 포인트를 설명할 수 있다.

- 결제 금액 위변조 방지
- 서버 승인 confirm
- 주문 상태 전이
- 결제 실패 시 재고 복구
- 중복 confirm 방지
- 결제 성공 후 장바구니 비우기

## 현재 프로젝트 상태

현재 백엔드 주문 흐름:

- `POST /api/orders`
- 장바구니를 주문으로 전환
- `OrderService.checkout()`
- `findByIdForUpdate()`로 상품을 잠금
- 재고 감소
- 주문 상태를 바로 `PAID`로 변경
- 장바구니 삭제

현재 구조의 문제:

- 실제 결제 PG가 없다.
- 주문 생성과 결제 완료가 한 번에 처리된다.
- 결제 실패/취소/승인 대기 상태를 표현하기 어렵다.
- 결제 금액 검증 테이블이 없다.

결제를 붙이려면 이 흐름을 둘로 나누는 것이 좋다.

변경 후 목표 흐름:

1. `POST /api/payments/ready`
   - 장바구니를 주문 초안으로 만든다.
   - 재고를 예약한다.
   - 주문 상태를 `PAYMENT_PENDING`으로 둔다.
   - 결제 금액, 결제용 주문번호를 만든다.
2. 프론트가 PG 결제창을 연다.
3. `POST /api/payments/confirm`
   - 프론트가 받은 `paymentKey`, `orderId`, `amount`를 백엔드로 보낸다.
   - 백엔드가 DB의 주문 금액과 비교한다.
   - 백엔드가 Toss/PortOne 서버에 승인 요청한다.
   - 성공하면 주문 `PAID`, 결제 `APPROVED`.
   - 장바구니를 비운다.

## 백엔드에 필요한 변화

### 1. 주문 상태 확장

현재:

```java
ORDERED, PAID, CANCELLED
```

추천:

```java
PAYMENT_PENDING,
PAID,
PAYMENT_FAILED,
CANCELLED,
REFUND_PENDING,
REFUNDED
```

처음에는 아래만 써도 된다.

```java
PAYMENT_PENDING,
PAID,
PAYMENT_FAILED,
CANCELLED
```

### 2. Payment 테이블 추가

새 엔티티 예시:

```text
Payment
- id
- order_id
- provider
- payment_order_id
- provider_payment_key
- amount
- status
- method
- requested_at
- approved_at
- failed_at
- failure_code
- failure_message
- receipt_url
- idempotency_key
- raw_response
```

중요 컬럼:

- `payment_order_id`: 우리 서비스가 만든 결제용 주문번호
- `provider_payment_key`: Toss/PortOne이 주는 결제 식별자
- `amount`: 결제해야 하는 금액
- `status`: `READY`, `APPROVED`, `FAILED`, `CANCELED`
- `idempotency_key`: 같은 승인 요청이 두 번 들어와도 한 번만 처리하기 위한 키

### 3. PaymentStatus enum 추가

예시:

```java
READY,
APPROVING,
APPROVED,
FAILED,
CANCELED,
REFUNDED
```

처음에는 단순하게 가도 된다.

```java
READY,
APPROVED,
FAILED,
CANCELED
```

### 4. PaymentProvider enum 추가

```java
TOSS,
PORTONE,
STRIPE,
MOCK
```

처음 구현은 `TOSS`와 `MOCK`만 있어도 충분하다.

### 5. PaymentGatewayClient 인터페이스

PG사 코드를 서비스 로직에 직접 박아 넣지 말고 인터페이스로 감싸는 것이 좋다.

예시:

```java
public interface PaymentGatewayClient {
    PaymentConfirmResult confirm(PaymentConfirmCommand command);
    PaymentCancelResult cancel(PaymentCancelCommand command);
}
```

그리고 구현체:

```text
TossPaymentGatewayClient
MockPaymentGatewayClient
```

이렇게 하면 테스트할 때 실제 Toss 서버를 안 부르고 `MockPaymentGatewayClient`로 검증할 수 있다.

## 프론트에 필요한 변화

### 1. 결제 시작 버튼

현재 장바구니의 주문 버튼은 바로 `POST /api/orders`를 호출한다.

결제 도입 후:

```text
장바구니 결제하기
-> POST /api/payments/ready
-> 결제창 열기
```

### 2. 결제 성공 페이지

라우트 예시:

```text
/payments/success
```

이 페이지는 URL query에서 PG가 넘겨준 값을 읽는다.

Toss 예시 개념:

```text
paymentKey
orderId
amount
```

그리고 백엔드에 보낸다.

```text
POST /api/payments/confirm
```

### 3. 결제 실패 페이지

라우트 예시:

```text
/payments/fail
```

실패 코드와 메시지를 보여주고, 주문을 `PAYMENT_FAILED`로 바꾸거나 예약 재고를 복구한다.

### 4. 결제 진행 중 상태

사용자가 confirm 버튼을 두 번 누르거나 새로고침할 수 있다.

프론트에서는:

- confirm 중 버튼 비활성화
- 성공/실패 banner
- 중복 요청에도 같은 결과를 보여주는 UX

백엔드에서는:

- idempotency 처리
- 이미 `PAID`인 주문은 다시 승인하지 않기

## 데이터 모델 추천안

### orders

추가 또는 변경:

```text
status: PAYMENT_PENDING / PAID / PAYMENT_FAILED / CANCELLED
order_date
paid_at
payment_expires_at
total_amount
```

`total_amount`는 없어도 계산할 수 있지만, 결제 검증을 위해 주문 생성 시점 금액을 저장하는 편이 좋다.

### order_item

현재 구조는 좋다.

이미 있음:

```text
item_id
order_price
quantity
order_id
```

주문 당시 가격인 `order_price`를 저장하고 있어서 결제 금액 검증에 유리하다.

### payment

새로 추가:

```text
id
order_id
provider
payment_order_id
provider_payment_key
amount
status
method
requested_at
approved_at
failed_at
failure_code
failure_message
receipt_url
idempotency_key
raw_response
```

### payment_event

처음에는 생략 가능하다.

웹훅까지 제대로 하고 싶으면 추가한다.

```text
id
provider
event_id
event_type
payload
received_at
processed_at
processing_status
```

왜 필요한가:

- PG사가 결제 성공/취소/환불 이벤트를 웹훅으로 다시 보내는 경우가 있다.
- 같은 웹훅이 여러 번 올 수 있으므로 `event_id` unique가 필요하다.

처음 구현에서는 1단계에서 생략하고, 2단계에서 붙여도 된다.

## 재고와 동시성은 어떻게 처리할까

드롭 쇼핑몰에서는 이게 중요하다.

### 방식 A: 결제 완료 후 재고 감소

쉬워 보이지만 위험하다.

문제:

- 사용자가 결제했는데 그 사이 재고가 없어질 수 있다.
- 그러면 결제는 됐는데 주문은 실패한다.
- 환불 로직까지 바로 필요해진다.

포트폴리오에서는 추천하지 않는다.

### 방식 B: 결제 시작 전에 재고 예약

추천.

흐름:

1. 사용자가 결제하기 클릭
2. 백엔드가 상품 row를 lock
3. 재고가 있으면 재고를 먼저 차감하거나 예약 처리
4. 주문 상태 `PAYMENT_PENDING`
5. 결제 성공 시 `PAID`
6. 결제 실패/시간 초과 시 재고 복구

현재 프로젝트는 이미 `findByIdForUpdate()`를 쓰고 있으므로 이 방식과 잘 맞는다.

주의:

- 결제창을 열고 사용자가 아무것도 안 하면 재고가 묶인다.
- 그래서 `payment_expires_at`이 필요하다.
- 만료된 `PAYMENT_PENDING` 주문은 배치나 수동 API로 취소하고 재고를 복구해야 한다.

처음에는 간단히:

- 결제 대기 만료 시간 10분
- confirm 실패 시 즉시 재고 복구
- 만료 복구 배치는 다음 단계로 미룸

## API 설계 초안

### 결제 준비

```http
POST /api/payments/ready
```

응답:

```json
{
  "paymentId": 1,
  "orderId": 10,
  "paymentOrderId": "DROP-20260626-000001",
  "orderName": "시티 레더 재킷 외 2건",
  "amount": 189000,
  "customerKey": "user-1",
  "provider": "TOSS"
}
```

### 결제 승인

```http
POST /api/payments/confirm
```

요청:

```json
{
  "paymentKey": "PG사가 준 키",
  "paymentOrderId": "DROP-20260626-000001",
  "amount": 189000
}
```

백엔드 검증:

- `paymentOrderId`로 payment 조회
- DB 금액과 요청 금액 비교
- 주문 상태가 `PAYMENT_PENDING`인지 확인
- 이미 승인된 결제면 기존 결과 반환
- PG confirm API 호출
- 성공하면 payment `APPROVED`, order `PAID`

### 결제 실패 처리

```http
POST /api/payments/fail
```

또는 실패 redirect 페이지에서 백엔드로:

```http
POST /api/payments/{paymentId}/fail
```

처리:

- payment `FAILED`
- order `PAYMENT_FAILED`
- 예약 재고 복구

### 결제 취소/환불

처음에는 주문 취소와 연결한다.

```http
POST /api/payments/{paymentId}/cancel
```

또는 기존:

```http
DELETE /api/orders/{id}
```

내부 처리:

- 이미 결제된 주문이면 PG cancel API 호출
- 성공하면 order `CANCELLED`
- 재고 복구

## 테스트 키를 어디서 받나

### Toss Payments

1. Toss Payments 개발자/상점 콘솔 가입
2. 테스트 상점 또는 테스트 키 확인
3. 클라이언트 키와 시크릿 키 확인
4. 프론트 `.env`에 client key
5. 백엔드 `.env`에 secret key

넣을 값:

```env
# shopping_fe/.env
VITE_PAYMENT_PROVIDER=TOSS
VITE_TOSS_CLIENT_KEY=test_...

# shopping_be/.env
PAYMENT_PROVIDER=TOSS
TOSS_SECRET_KEY=test_...
TOSS_SUCCESS_URL=http://localhost:5173/payments/success
TOSS_FAIL_URL=http://localhost:5173/payments/fail
```

주의:

- `VITE_TOSS_CLIENT_KEY`는 브라우저에 노출된다.
- `TOSS_SECRET_KEY`는 절대 프론트에 넣으면 안 된다.
- 백엔드에는 일반적으로 `TOSS_CLIENT_KEY`가 필요 없다. 결제창을 여는 공개 키는 프론트에서만 사용하고, 백엔드는 승인/취소용 secret key만 사용한다.
- 백엔드 `.env`도 Git에 올리면 안 된다.

### PortOne

1. PortOne 콘솔 가입
2. 테스트 채널 설정
3. Store ID, Channel Key 확인
4. API Secret 확인
5. 필요하면 Webhook Secret 설정

예시:

```env
# shopping_fe/.env
VITE_PAYMENT_PROVIDER=PORTONE
VITE_PORTONE_STORE_ID=store-...
VITE_PORTONE_CHANNEL_KEY=channel-key-...

# shopping_be/.env
PAYMENT_PROVIDER=PORTONE
PORTONE_API_SECRET=...
PORTONE_WEBHOOK_SECRET=...
```

### Stripe

1. Stripe Dashboard 가입
2. Test mode에서 Publishable key 확인
3. Secret key 확인
4. Webhook을 쓰면 webhook signing secret 확인

예시:

```env
# shopping_fe/.env
VITE_PAYMENT_PROVIDER=STRIPE
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...

# shopping_be/.env
PAYMENT_PROVIDER=STRIPE
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

## 처음 구현 단계 추천

### 1단계: Mock 결제

목표:

- PG 없이 결제 상태 모델과 주문 상태 전이를 먼저 만든다.

구현:

- `Payment` 엔티티 추가
- `PaymentService` 추가
- `POST /api/payments/ready`
- `POST /api/payments/mock-confirm`
- 프론트 성공/실패 페이지 구성

장점:

- PG 문서에 막히지 않고 도메인 흐름을 먼저 잡는다.
- 테스트를 탄탄하게 만들 수 있다.

### 2단계: Toss 테스트 결제

목표:

- Mock confirm 대신 Toss confirm API를 호출한다.

구현:

- `TossPaymentGatewayClient`
- Toss client key로 프론트 결제창 실행
- success redirect에서 백엔드 confirm 호출
- 금액 검증

### 3단계: 실패/취소/만료 처리

목표:

- 실제 서비스스러운 예외 흐름 구현

구현:

- 결제 실패 시 재고 복구
- 결제 대기 만료 주문 복구
- 결제 취소/환불
- 중복 confirm idempotency

### 4단계: 웹훅

목표:

- PG 서버 이벤트를 백엔드가 직접 받는다.

구현:

- `/api/payments/webhooks/toss`
- signature 검증
- 이벤트 중복 처리
- payment_event 테이블

처음부터 웹훅까지 하면 범위가 커진다. 포트폴리오 1차 목표는 2단계까지만 해도 충분하다.

## 테스트 계획

### 백엔드 단위 테스트

필수:

- 장바구니가 비어 있으면 결제 준비 실패
- 재고 부족이면 결제 준비 실패
- 결제 준비 시 재고가 예약됨
- 결제 승인 성공 시 주문 `PAID`
- 결제 승인 금액이 DB 금액과 다르면 실패
- 결제 승인 실패 시 주문 `PAYMENT_FAILED`
- 결제 실패 시 재고 복구
- 같은 confirm 요청이 두 번 들어와도 결제는 한 번만 처리

### 백엔드 통합 테스트

필수:

- `POST /api/payments/ready`
- `POST /api/payments/confirm`
- 인증되지 않은 사용자는 401
- 다른 사용자의 payment confirm은 403

### 프론트 테스트

필수:

- 장바구니에서 결제 준비 API 호출
- 결제 성공 페이지가 query string을 읽고 confirm 호출
- confirm 성공 후 완료 화면
- confirm 실패 후 실패 화면
- 중복 클릭 방지

### 수동 테스트

Toss 테스트 키를 넣은 뒤 확인:

- 결제창 열림
- 성공 redirect 도착
- 백엔드 confirm 성공
- 주문 상태 `PAID`
- 장바구니 비움
- 재고 감소 유지
- 실패 redirect 처리

## 포트폴리오에서 설명할 수 있는 포인트

- 프론트 결제 성공값을 신뢰하지 않고 백엔드에서 PG confirm 수행
- DB 주문 금액과 PG 요청 금액 비교로 금액 위변조 방지
- 결제 준비 단계에서 재고를 lock하여 race condition 대응
- 결제 실패/만료 시 재고 복구
- idempotency key로 중복 승인 방지
- PG client를 인터페이스로 감싸 테스트 가능하게 설계
- Mock 결제와 실제 Toss 결제를 profile/env로 분리

## 내가 고를 선택

내가 이 프로젝트를 이어서 구현한다면 순서는 이렇게 간다.

1. `Payment` 모델과 `MOCK` 결제 흐름 구현
2. 결제 준비/승인 API 테스트 작성
3. 프론트 성공/실패 페이지 구현
4. Toss 테스트 키 발급 후 실제 결제창 연결
5. 결제 실패/취소/만료 재고 복구 구현

처음부터 Toss를 바로 붙일 수도 있지만, 결제는 PG보다 **주문 상태와 재고 상태**가 더 중요하다. 그래서 Mock으로 도메인 흐름을 먼저 세우고 Toss를 붙이는 방식이 가장 덜 헷갈린다.

## 결정 체크리스트

아래 질문에 답하면 다음 구현 방향이 정해진다.

- 국내 쇼핑몰 느낌이 중요한가? 그렇다면 Toss.
- 여러 PG 확장성을 보여주고 싶은가? 그렇다면 PortOne.
- 글로벌 결제 데모와 문서 친화성이 중요한가? 그렇다면 Stripe.
- 지금 당장 PG 키 없이 구현을 시작하고 싶은가? 그렇다면 Mock 결제부터.
- 드롭 상품의 선착순/동시성을 강조하고 싶은가? 그렇다면 결제 준비 단계에서 재고 예약 방식.

## 참고 링크

- Toss Payments Docs: https://docs.tosspayments.com/
- Toss Payments Payment Widget: https://docs.tosspayments.com/en/integration-widget
- PortOne Developers: https://developers.portone.io/
- Stripe API Keys: https://docs.stripe.com/keys
- Stripe Testing: https://docs.stripe.com/testing
