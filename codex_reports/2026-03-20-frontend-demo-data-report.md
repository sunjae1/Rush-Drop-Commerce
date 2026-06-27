# 2026-03-20 프론트 더미 데이터 전환 보고서

## 1. 작업 목적

- 백엔드 연결 없이도 쇼핑몰 화면과 주요 사용자 흐름을 바로 확인할 수 있도록 프론트 전용 더미 모드를 추가했습니다.
- 실제 API를 완전히 끊어버린 것이 아니라, 환경변수로 데모 모드와 실서버 모드를 전환할 수 있게 구성했습니다.
- 상품 이미지는 무료 사용 기준의 Pexels 이미지를 사용해 데모 품질을 올렸습니다.

## 2. 이번에 구현한 내용

### 2-1. 프론트 전용 데모 API 추가

변경 파일:

- `shopping_fe/src/api/demoSeed.ts`
- `shopping_fe/src/api/demoClient.ts`
- `shopping_fe/src/api/client.ts`

핵심 내용:

- `demoSeed.ts`에 카테고리, 상품, 사용자, 장바구니, 주문, 게시글, 댓글 초기 데이터를 넣었습니다.
- `demoClient.ts`에서 실제 백엔드 API 대신 동작하는 프론트 전용 로직을 만들었습니다.
- 데이터 저장은 브라우저 `localStorage`를 사용합니다.
- 따라서 페이지를 새로고침해도 데모 상태가 유지되고, 로컬 스토리지만 지우면 초기 상태로 되돌릴 수 있습니다.

### 2-2. 데모 모드 전환 방식

변경 파일:

- `shopping_fe/.env.development`
- `shopping_fe/.env.production`

핵심 내용:

- `VITE_USE_DEMO_DATA=true` 이면 프론트가 실제 `/api/*` 대신 데모 클라이언트를 사용합니다.
- 개발 환경에서는 데모 모드가 켜지도록 설정했습니다.
- 배포 환경에서는 `VITE_USE_DEMO_DATA=false` 로 실제 API를 사용하도록 유지했습니다.

정리하면:

- 개발 확인용: 데모 모드 ON
- 실제 백엔드 연결용: 데모 모드 OFF

### 2-3. 데모 계정 안내

변경 파일:

- `shopping_fe/src/pages/LoginPage.tsx`

핵심 내용:

- 로그인 페이지에 데모 계정 안내 문구를 추가했습니다.
- 바로 테스트 가능한 계정은 아래와 같습니다.

데모 계정:

- 사용자: `demo@seoulselect.com / demo123!`
- 관리자: `admin@seoulselect.com / admin123!`

## 3. 데모 데이터 구성

### 3-1. 카테고리

- 아우터
- 상의
- 하의
- 슈즈

### 3-2. 상품

총 7개의 샘플 상품을 넣었습니다.

- 스테이플 그래픽 티셔츠
- 레더 클래식 재킷
- 샌드 트렌치 코트
- 코지 블루 니트
- 스트레이트 데님 팬츠
- 화이트 데일리 스니커즈
- 어반 블랙 후디

### 3-3. 기타 데이터

- 장바구니 샘플 데이터
- 주문 내역 샘플 데이터
- 커뮤니티 게시글/댓글 샘플 데이터
- 관리자 계정 포함

즉 홈, 상품 상세, 장바구니, 마이페이지, 커뮤니티, 관리자 화면까지 한 번에 데모 확인이 가능하도록 구성했습니다.

## 4. 이미지 정책

- 상품 이미지는 무료 사용 기준의 Pexels 이미지를 사용했습니다.
- 현재는 데모 품질 확보가 목적이므로 외부 URL을 직접 참조합니다.
- 이후 실제 S3를 연결하면 `imageUrl`만 S3 주소로 교체하면 됩니다.

참고 링크:

- [Pexels License](https://www.pexels.com/license/)
- [Pexels Graphic T-shirt Photo](https://www.pexels.com/photo/stylish-man-in-black-graphic-t-shirt-33258835/)
- [Pexels Leather Jacket Photo](https://www.pexels.com/photo/a-man-in-a-black-leather-jacket-12148300/)

## 5. 실제 전환 시 의미

이번 작업은 어디까지나 프론트 시연용 더미 모드입니다.

- 지금은 프론트만으로도 쇼핑몰처럼 화면을 확인할 수 있습니다.
- 이후 실제 S3와 RDS를 연결하면, 데모 모드를 끄고 백엔드 데이터를 연결하면 됩니다.
- 즉 프론트 구조를 다시 갈아엎을 필요 없이 데이터 소스만 실서버로 바꾸면 됩니다.

## 6. 검증

실행한 검증:

- `npm test`
- `npm run build`
- `npx vite build --mode development`

결과:

- Vitest 전체 통과
- production build 통과
- development mode build 통과

## 7. 최종 요약

> 프론트만으로 동작하는 더미 쇼핑몰 모드를 추가했고, 무료 이미지 기반 샘플 상품/카테고리/주문/게시글 데이터까지 넣어 두었습니다. 이후 실제 S3/RDS/API를 연결할 때는 `VITE_USE_DEMO_DATA`만 꺼서 실데이터 모드로 전환하면 됩니다.
