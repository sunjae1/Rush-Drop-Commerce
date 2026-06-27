# 002 Frontend Drop Redesign Report

작성일: 2026-06-25

## 목표

프론트엔드를 기존 일반 쇼핑몰/편집샵 톤에서 `선착순 한정 수량 드롭 쇼핑몰` 도메인에 맞게 개편했다.

이번 작업은 서버 동시성 제어나 결제 플로우 구현이 아니라, 이미 API에 노출된 드롭 상품 필드를 화면 구조와 사용자 흐름에 반영하는 것이 목적이다.

## 변경 요약

### 1. 홈 화면 정보 구조 개편

Before:

- 일반 쇼핑몰처럼 시즌 셀렉션, 카테고리, 가격대 상위 상품, 재고 임박 상품을 보여줌
- 드롭 상품 여부가 첫 화면에서 강하게 드러나지 않음
- 사용자가 “지금 구매 가능한가”, “언제 열리나”를 바로 판단하기 어려움

After:

- 히어로를 `선착순 한정 수량 드롭 쇼핑몰` 메시지로 변경
- 홈 상단 지표를 `진행 중 드롭`, `오픈 예정`, `구매 가능 재고`로 변경
- `오늘의 드롭 운영 보드` 섹션 추가
  - 지금 구매 가능한 드롭
  - 오픈 대기 스케줄
- 기존 카테고리/카탈로그도 `드롭 카테고리`, `전체 드롭 카탈로그` 문맥으로 변경

프론트엔드 관점:

- Information Architecture를 일반 상품 탐색 중심에서 드롭 상태 판단 중심으로 변경
- Above the fold에서 도메인 정체성이 바로 읽히도록 hero messaging과 KPI를 재정렬
- API에서 내려오는 `dropProduct`, `dropSaleStatus`, `dropStartsAt`, `dropEndsAt`를 홈 UI에 직접 반영

### 2. 상품 카드 개선

Before:

- 상품 상태는 `판매중`, `수량 한정`, `품절` 중심
- 드롭 일정은 카드에서 보이지 않음

After:

- 드롭 상품이면 `드롭 예정`, `드롭 진행`, `드롭 종료` 배지를 우선 노출
- 카드 안에 드롭 일정 텍스트 표시
- CTA를 드롭 상품 기준으로 `드롭 상세`로 변경

프론트엔드 관점:

- Product card의 status badge와 CTA affordance를 도메인에 맞게 변경
- 사용자가 리스트에서 상세 진입 전에 오픈 상태를 판단할 수 있게 함

### 3. 상품 상세 개선

Before:

- “데일리 룩에 어울리는 아이템” 같은 일반 상품 설명 중심
- 구매 버튼 비활성화는 있었지만, 왜 막혔는지 설명이 약함

After:

- 상세 설명을 드롭 상품 기준으로 변경
- 드롭 상품이면 상태별 안내 박스 추가
  - 진행 중: 지금 구매 가능한 드롭
  - 예정: 아직 오픈 전
  - 종료: 종료된 드롭
- `1인 제한` 정보를 상세 통계에 표시

프론트엔드 관점:

- Disabled CTA의 reason을 UI copy로 설명해 사용자의 다음 행동을 명확히 함
- Detail page를 단순 상품 설명 페이지가 아니라 purchase readiness 판단 화면으로 변경

### 4. 관리자 화면 개선

Before:

- `상품 관리`라는 일반 CRUD 관리자 문맥
- 드롭 필드는 있었지만 운영 목적이 강하게 보이지 않음

After:

- `드롭 상품 운영`으로 타이틀 변경
- 등록/수정 폼을 `드롭 상품 등록`, `드롭 상품 수정` 문맥으로 변경
- 관리자 통계에 현재 조건의 드롭 상품 수 표시

프론트엔드 관점:

- Admin UX copy를 CRUD가 아니라 operations workflow 기준으로 변경
- 운영자가 상품을 “등록”하는 것이 아니라 드롭 시간/제한을 “운영”한다는 맥락을 강화

### 5. 앱 셸 브랜딩 변경

Before:

- `Seoul Select Mall`
- `CURATED STOREFRONT`
- 일반 편집샵/큐레이션 몰 느낌

After:

- `Seoul Drop Market`
- `LIMITED DROP MARKET`
- 유틸리티 바에서도 한정 수량/선착순/오픈 대기 문맥 노출

프론트엔드 관점:

- Global navigation과 brand lockup의 vocabulary를 도메인과 일치시킴
- 사용자가 어느 페이지에 있어도 서비스 성격을 잃지 않게 함

## 사용한 API/데이터

화면에서 활용한 기존 API 데이터:

- `GET /api/items`
  - `dropProduct`
  - `dropSaleStatus`
  - `dropStartsAt`
  - `dropEndsAt`
  - `dropPurchaseLimit`
  - `quantity`
  - `price`
  - `categoryId`
  - `categoryName`
- `GET /api/categories`
  - 카테고리 카드와 필터
- `GET /api/posts`
  - 드롭 후기 피드

데모 데이터도 보강했다.

- 진행 중 드롭
- 오픈 예정 드롭
- 종료된 드롭

## 변경 파일

- `shopping_fe/src/pages/HomePage.tsx`
- `shopping_fe/src/pages/ProductPage.tsx`
- `shopping_fe/src/pages/AdminItemsPage.tsx`
- `shopping_fe/src/components/ProductCard.tsx`
- `shopping_fe/src/components/AppShell.tsx`
- `shopping_fe/src/api/demoSeed.ts`
- `shopping_fe/src/styles/app.css`
- `shopping_fe/src/test/home-page.test.tsx`

## 검증

실행 명령:

```bash
npm run build
```

결과:

- 성공
- TypeScript type check 통과
- Vite production build 통과

실행 명령:

```bash
npm test
```

결과:

- 성공
- 13개 test file 통과
- 40개 test 통과

참고:

- React Router v7 future flag 경고는 기존 경고이며 실패 원인은 아니다.

## 남은 한계

이번 작업은 프론트 도메인 개편이다.

아직 서버에서 다음 로직을 보장하지 않는다.

- 드롭 오픈 전 장바구니 API 차단
- 드롭 종료 후 장바구니 API 차단
- 1인 구매 제한 서버 검증
- 선착순 재고 race condition 방지
- 결제 대기/실패/복구

따라서 다음 백엔드 단계에서는 `CartService` 또는 주문 API에서 드롭 상태와 구매 제한을 서버 규칙으로 강제해야 한다.
