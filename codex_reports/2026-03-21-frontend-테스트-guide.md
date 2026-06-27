# 프론트엔드 테스트 전체 가이드

작성일: 2026-03-21  
대상: 프론트 초보자  
기준 프로젝트: `shopping_fe`

## 1. 이 문서는 무엇을 설명하나

이 프로젝트에는 테스트가 한 종류만 있는 것이 아니라, 목적이 다른 여러 검증 단계가 있습니다.

- `npm run build`: 빌드가 되는지 확인
- `npm test`: 컴포넌트/함수 단위 동작 확인
- `npm run test:e2e`: 브라우저에서 실제 사용자 흐름 확인
- `npm run test:e2e:visual`: 화면이 예전과 똑같이 보이는지 시각 비교

핵심은 이겁니다.

- 빌드는 "코드가 묶여서 배포 가능한가?"
- Vitest는 "컴포넌트와 함수가 의도대로 동작하는가?"
- Playwright E2E는 "브라우저에서 실제 흐름이 이어지는가?"
- 시각 회귀 테스트는 "레이아웃과 화면 모양이 깨지지 않았는가?"

즉, 하나만 돌린다고 전부 확인되는 구조가 아닙니다.

---

## 2. 한눈에 보는 표

| 명령어 | 도구 | 실행 위치/환경 | 주로 검증하는 것 | 이런 때 돌리면 좋음 |
|---|---|---|---|---|
| `npm run build` | TypeScript + Vite | 빌드 환경 | 타입 오류, import 오류, production build 가능 여부 | 작업 마무리 전 항상 |
| `npm test` | Vitest + Testing Library + jsdom | Node + 가상 DOM | 컴포넌트 렌더링, 상태 변화, 함수 로직 | 작은 UI/로직 수정 후 |
| `npm run test:watch` | Vitest | Node + 가상 DOM | 위와 같음, 저장하며 반복 확인 | 개발 중 계속 확인할 때 |
| `npm run test:e2e` | Playwright | 실제 Chromium 브라우저 | 로그인, 이동, 장바구니, 폼 제출 같은 기능 흐름 | 기능 연결이 바뀌었을 때 |
| `npm run test:e2e:headed` | Playwright | 브라우저 창을 띄운 실제 Chromium | 위와 같음, 눈으로 같이 보기 좋음 | 디버깅할 때 |
| `npm run test:e2e:visual` | Playwright screenshot diff | 실제 브라우저 + 기준 이미지 비교 | 레이아웃 깨짐, 간격 변화, 드로어 가림, 화면 구조 변화 | CSS/반응형 작업 후 |
| `npm run test:e2e:visual:update` | Playwright screenshot diff | 실제 브라우저 + baseline 갱신 | 의도적으로 바뀐 새 화면을 기준 이미지로 저장 | 디자인 변경을 확정했을 때 |

---

## 3. 먼저 이해해야 하는 큰 차이

### 3-1. `build`는 테스트인가?

엄밀히 말하면 `build`는 테스트 프레임워크 테스트는 아닙니다.  
하지만 실무에서는 가장 중요한 사전 검증 중 하나입니다.

이 프로젝트의 `build` 스크립트는 다음을 합니다.

```json
"build": "tsc --noEmit -p tsconfig.app.json && tsc --noEmit -p tsconfig.node.json && vite build"
```

즉:

1. TypeScript 타입 검사
2. Node/Vite 관련 타입 검사
3. 실제 production bundle 생성

그래서 다음 같은 문제를 잡습니다.

- 잘못된 타입 사용
- 없는 파일 import
- 배포 빌드 시 깨지는 설정 문제

하지만 다음은 못 잡습니다.

- 버튼이 안 눌리는 문제
- 라우팅 흐름 문제
- 화면이 가려지는 문제

---

## 4. `npm test`: Vitest는 어떤 원리로 돌까

### 4-1. 실행 원리

이 프로젝트의 Vitest 설정은 `shopping_fe/vite.config.ts`에 있습니다.

```ts
test: {
  environment: "jsdom",
  setupFiles: "./vitest.setup.ts",
  include: ["src/test/**/*.test.ts", "src/test/**/*.test.tsx"]
}
```

여기서 중요한 건 `jsdom`입니다.

`jsdom`은 "진짜 브라우저"는 아니지만, 브라우저처럼 DOM이 있는 척 해주는 가상 환경입니다.  
그래서 테스트 안에서 React 컴포넌트를 렌더링하고, 버튼 클릭이나 텍스트 표시 여부를 확인할 수 있습니다.

흐름은 보통 이렇습니다.

1. 테스트 파일 실행
2. React 컴포넌트를 메모리 안에 렌더링
3. `screen.getByRole(...)` 같은 방식으로 요소 찾기
4. `fireEvent.click(...)` 같은 방식으로 상호작용
5. 기대한 결과가 화면에 나타나는지 확인

즉, 브라우저를 띄우지는 않지만 "컴포넌트가 어떻게 반응하는지" 빠르게 검증합니다.

### 4-2. 실제 이 프로젝트에서는 무엇을 검증하나

테스트 파일은 `shopping_fe/src/test` 아래에 있습니다.

- `home-page.test.tsx`
- `session-shell.test.tsx`
- `product-page.test.tsx`
- `product-card.test.tsx`
- `community-detail-page.test.tsx`
- `admin-categories-page.test.tsx`
- `auth.test.ts`
- `api-client-auth.test.ts`
- `format.test.ts`

예를 들어 `home-page.test.tsx`는 이런 식으로 동작합니다.

```ts
vi.mock("../api/client", () => ({
  fetchItems: vi.fn(),
  fetchCategories: vi.fn(),
  fetchPosts: vi.fn()
}));
```

이 코드는 진짜 서버를 부르지 않고, API 함수를 가짜로 바꿉니다.  
그래서 테스트는 빠르고 안정적으로 돌 수 있습니다.

그리고 이렇게 렌더링합니다.

```tsx
render(
  <MemoryRouter>
    <HomePage />
  </MemoryRouter>
);
```

`MemoryRouter`는 테스트용 라우터입니다.  
실제 브라우저 주소창 없이도 페이지 이동이 있는 컴포넌트를 검사할 수 있게 해줍니다.

검증 예시는 이런 것들입니다.

- 홈의 대표 상품(hero)이 날짜 기준으로 바뀌는지
- 검색 입력 시 상품 리스트가 필터링되는지
- 카테고리 버튼 클릭 시 해당 카테고리만 보이는지
- 검색할 때 스크롤 보정이 되는지

즉 Vitest는 "이 컴포넌트가 이 입력과 상태에서 올바르게 반응하나?"를 보는 테스트입니다.

### 4-3. `session-shell.test.tsx`는 왜 중요한가

이 파일은 단순히 글자 하나 보이는지만 확인하지 않고, 여러 컨텍스트와 라우팅을 함께 봅니다.

- `SessionProvider`
- `CartProvider`
- `Routes`
- `AppShell`

그리고 모바일 메뉴도 확인합니다.

```ts
mockMatchMedia(true);
```

이렇게 모바일 화면인 척 만든 뒤,

- 햄버거 버튼이 보이는지
- 눌렀을 때 메뉴가 열리는지
- 다시 눌렀을 때 닫히는지

를 확인합니다.

즉 이 테스트는 "컴포넌트 단위"이지만, 실제 앱 구조와 꽤 비슷한 조합으로 검증하는 편입니다.

### 4-4. Vitest가 잘하는 것과 한계

잘하는 것:

- 빠름
- 작은 로직 확인에 좋음
- API를 mock 처리하기 쉬움
- 특정 상태를 정밀하게 재현하기 쉬움

한계:

- 진짜 브라우저 렌더링은 아님
- 실제 클릭 흐름, 주소 이동, 브라우저 레이아웃 차이는 E2E보다 약함
- 픽셀 단위 시각 비교는 하지 않음

---

## 5. `npm run test:e2e`: Playwright 기능 E2E는 어떤 원리로 돌까

### 5-1. E2E의 뜻

E2E는 `End-to-End`의 줄임말입니다.  
사용자가 실제로 하는 흐름을 처음부터 끝까지 브라우저에서 따라가 보는 테스트입니다.

예:

- 홈 진입
- 로그인 페이지 이동
- 로그인
- 마이페이지 진입
- 상품 상세 진입
- 장바구니 담기
- 주문

### 5-2. Playwright는 내부에서 어떻게 동작하나

`shopping_fe/playwright.config.ts`를 보면 이런 설정이 있습니다.

```ts
webServer: {
  command: "npm run dev -- --host 127.0.0.1 --port 4173",
  url: "http://127.0.0.1:4173",
  reuseExistingServer: true
}
```

이 뜻은:

1. Playwright가 테스트 전에 Vite dev 서버를 띄움
2. `http://127.0.0.1:4173`으로 접속
3. 실제 Chromium 브라우저를 열어서 테스트 실행

즉 E2E는 "브라우저 없이 흉내"가 아니라, 브라우저를 실제로 띄워서 페이지를 조작합니다.

### 5-3. 그런데 백엔드는 어떻게 하나

이 프로젝트는 E2E에서도 실제 백엔드를 직접 치지 않고, `mockApi.ts`를 씁니다.

핵심 부분은 이 코드입니다.

```ts
await page.route("**/*", async (route) => {
  await handleRequest(route, state);
});
```

즉 브라우저에서 나가는 요청을 Playwright가 가로채고,

- `/api/login`
- `/api/items`
- `/api/cart`
- `/api/orders`

같은 요청에 대해 가짜 응답을 돌려줍니다.

이 구조의 장점은:

- 백엔드 서버 상태에 덜 흔들림
- 테스트 재현성이 높음
- 로그인, 장바구니, 주문 흐름을 안정적으로 검증 가능

중요한 점은:

- 프론트 화면과 브라우저 동작은 진짜
- API 응답만 가짜

즉 "브라우저 E2E + mock backend" 구조입니다.

### 5-4. 실제 E2E 파일들은 무엇을 검증하나

`shopping_fe/e2e` 아래 주요 파일:

- `auth-flow.spec.ts`
- `public-navigation.spec.ts`
- `commerce.spec.ts`
- `community.spec.ts`
- `admin-items.spec.ts`
- `refresh-lock.spec.ts`

예를 들어 `auth-flow.spec.ts`는 이런 흐름을 봅니다.

- 비로그인 사용자가 `/account` 진입
- 로그인 페이지로 리다이렉트되는지
- 로그인 후 원래 가려던 페이지로 돌아가는지
- 만료된 access token이 refresh로 복구되는지

`commerce.spec.ts`는 이런 흐름을 봅니다.

- 상품 상세 진입
- 수량 증가
- 장바구니 담기
- 주문하기
- 마이페이지에서 주문 내역 보이는지
- 프로필 수정
- 로그아웃

`public-navigation.spec.ts`는 이런 흐름을 봅니다.

- 홈 진입
- 카테고리 필터
- 검색
- 상품 상세 이동
- 커뮤니티 진입
- 404 페이지 확인

즉 기능 E2E는 "사용자가 실제로 할 행동이 전체 흐름에서 이어지는가"를 확인합니다.

### 5-5. `headed`는 무엇이 다르나

```bash
npm run test:e2e:headed
```

이 명령은 브라우저 창을 눈으로 보이게 띄웁니다.

보통 `headless`는 브라우저 UI 없이 백그라운드 실행이고,  
`headed`는 실제 창이 떠서 클릭 흐름을 보며 디버깅하기 좋습니다.

즉 기능은 거의 같고, "내가 눈으로 같이 보며 디버깅하느냐"가 차이입니다.

### 5-6. E2E가 잘하는 것과 한계

잘하는 것:

- 실제 사용자 흐름 검증
- 라우팅/폼/클릭/리다이렉트 확인
- 브라우저 환경에서 실행

한계:

- Vitest보다 느림
- 세세한 로직 조합 확인은 단위 테스트보다 비효율적일 수 있음
- 텍스트가 보인다고 해서 레이아웃이 완벽하다는 뜻은 아님

---

## 6. `npm run test:e2e:visual`: 시각 회귀 테스트는 어떤 원리로 돌까

### 6-1. 시각 회귀 테스트란

이 테스트는 "기능이 되는가"보다 "화면이 예전과 같은 모양인가"를 비교합니다.

예를 들면 이런 문제를 잡는 데 강합니다.

- 드로어가 카드 뒤로 깔리는 문제
- 제목과 리스트 간격이 갑자기 달라진 문제
- 모바일에서 버튼이 화면 밖으로 밀리는 문제
- 태블릿에서 카드가 줄바꿈되며 깨지는 문제

### 6-2. 원리

흐름은 간단합니다.

1. 기준 스크린샷(baseline)을 미리 저장
2. 테스트 실행 시 현재 화면 스크린샷을 다시 찍음
3. 두 이미지를 비교
4. 차이가 허용 범위를 넘으면 실패

이 프로젝트는 `visual-regression.spec.ts`에서 이를 수행합니다.

```ts
await expect(page).toHaveScreenshot("home-top-shell.png", {
  ...visualScreenshotOptions
});
```

여기서 `home-top-shell.png`가 기준 이미지 이름입니다.

### 6-3. 시각 비교가 흔들리지 않게 왜 보정 코드를 넣었나

스크린샷 테스트는 날짜, 애니메이션, 폰트 로딩 상태에 따라 자주 흔들릴 수 있습니다.  
그래서 `e2e/fixtures/visual.ts`에서 안정화 작업을 합니다.

대표적으로:

- 날짜 고정
- `reducedMotion` 적용
- 네트워크 idle 대기
- 폰트 로딩 완료 대기
- 이미지 로딩 완료 대기
- 포커스 blur 처리

예:

```ts
export const visualScreenshotOptions = {
  animations: "disabled",
  caret: "hide",
  scale: "css",
  maxDiffPixelRatio: 0.01
};
```

즉 "깜빡이는 커서", "애니메이션 중간 프레임", "시간 표시 차이" 같은 흔들림을 줄여서  
의미 있는 UI 변화만 잡으려는 구조입니다.

### 6-4. 이 프로젝트는 어떤 화면을 시각 비교하나

현재 이 저장소는 다음 같은 화면을 baseline으로 저장하고 있습니다.

- 홈 상단 셸
- 홈 머천다이징 섹션
- 로그인
- 회원가입
- 상품 상세
- 커뮤니티 목록
- 커뮤니티 상세
- 장바구니
- 마이페이지
- 관리자 상품 관리
- 관리자 카테고리 관리
- 404 화면
- 모바일 드로어 열림 상태

### 6-5. PC / 태블릿 / 모바일은 어떻게 나누나

`playwright.config.ts`에서 시각 회귀 프로젝트를 따로 분리해두었습니다.

- `visual-desktop`
- `visual-tablet`
- `visual-mobile`

즉 같은 테스트 파일이라도 viewport를 바꿔서 각각 따로 스크린샷을 찍습니다.

그래서 같은 홈 화면이라도:

- 데스크톱 모양
- 태블릿 모양
- 모바일 모양

을 각각 비교할 수 있습니다.

### 6-6. `test:e2e:visual:update`는 언제 쓰나

```bash
npm run test:e2e:visual:update
```

이 명령은 기준 스크린샷을 새 화면으로 덮어씁니다.

즉:

- 의도하지 않은 깨짐이면 쓰면 안 됨
- 디자인을 일부러 바꿨고, 그 화면이 맞다면 그때 갱신

초보자가 많이 헷갈리는 포인트는 이겁니다.

"실패하니까 그냥 update 해버리면 되지 않나?"

이렇게 하면 진짜 깨진 화면도 새 기준으로 저장돼 버릴 수 있습니다.  
그래서 반드시 "이 변화가 의도한 UI 변경인지" 먼저 확인하고 갱신해야 합니다.

### 6-7. 시각 회귀가 잘하는 것과 한계

잘하는 것:

- 레이아웃 깨짐 탐지
- 반응형 화면 차이 탐지
- CSS 수정 후 미세한 간격/정렬 변화 탐지

한계:

- 왜 깨졌는지 원인까지 자동 설명하진 않음
- 데이터가 많이 달라지는 화면은 baseline 관리가 어려울 수 있음
- 텍스트만 조금 바뀌어도 실패할 수 있음

---

## 7. 초보자가 특히 헷갈리기 쉬운 것들

### 7-1. E2E가 있는데 왜 시각 테스트가 또 필요한가

기능 E2E는 보통 이런 걸 확인합니다.

- 버튼 클릭 가능
- URL 이동
- 텍스트 보임
- 저장/주문/로그인 흐름

하지만 아래 문제는 놓칠 수 있습니다.

- 메뉴가 보이긴 하지만 뒤에 깔려 있음
- 버튼이 보이긴 하지만 위치가 이상함
- 간격이 너무 좁아짐
- 카드가 겹침

즉:

- 기능 E2E = "동작하는가"
- 시각 회귀 = "보기에도 정상인가"

둘은 겹치는 부분이 있지만 완전히 같은 테스트가 아닙니다.

### 7-2. Playwright 설정의 `screenshot: "only-on-failure"`는 시각 회귀 테스트인가

아닙니다.

이 설정은 테스트가 실패했을 때 증거용 스크린샷을 남기는 기능입니다.

```ts
screenshot: "only-on-failure"
```

이건 "비교"가 아니라 "실패 화면 저장"입니다.

반면 시각 회귀 테스트는 `toHaveScreenshot()`으로 기준 이미지와 비교하는 방식입니다.

즉 둘은 완전히 다릅니다.

### 7-3. mock API를 쓰면 E2E가 가짜 아닌가

반은 맞고 반은 아닙니다.

- 브라우저는 진짜
- 화면 렌더링도 진짜
- 클릭/입력/라우팅도 진짜
- 서버 응답만 가짜

즉 프론트엔드 입장에서는 실제 사용자 흐름을 꽤 현실적으로 재현하면서,  
백엔드 상태 때문에 테스트가 흔들리지 않게 만든 구조입니다.

---

## 8. 현재 프로젝트의 테스트를 "지도처럼" 보면

### 8-1. 빠른 컴포넌트/로직 검증

도구:

- `Vitest`
- `@testing-library/react`
- `jsdom`

대표 파일:

- `src/test/home-page.test.tsx`
- `src/test/session-shell.test.tsx`
- `src/test/product-page.test.tsx`
- `src/test/format.test.ts`

보는 것:

- 렌더링 결과
- 상태 변화
- 이벤트 반응
- 유틸 함수 결과

### 8-2. 실제 사용자 흐름 검증

도구:

- `Playwright`

대표 파일:

- `e2e/auth-flow.spec.ts`
- `e2e/commerce.spec.ts`
- `e2e/public-navigation.spec.ts`
- `e2e/community.spec.ts`

보는 것:

- 페이지 이동
- 로그인/회원가입
- 장바구니/주문
- 커뮤니티 흐름
- 보호 페이지 접근

### 8-3. 반응형/레이아웃 시각 검증

도구:

- `Playwright screenshot diff`

대표 파일:

- `e2e/visual-regression.spec.ts`

보는 것:

- 화면 전체 구조
- 반응형 레이아웃
- 드로어/오버레이 위치
- 섹션 간격
- 페이지별 껍데기(shell)

---

## 9. 실제로는 어떤 순서로 돌리면 좋나

### 9-1. 작은 컴포넌트나 CSS 수정

추천 순서:

1. `npm run build`
2. `npm test`
3. 레이아웃 변경이 있으면 `npm run test:e2e:visual`

예:

- 마진 수정
- 버튼 스타일 수정
- 카드 UI 변경
- 반응형 헤더 수정

### 9-2. 페이지 흐름이나 기능 수정

추천 순서:

1. `npm run build`
2. `npm test`
3. `npm run test:e2e`
4. UI도 달라졌다면 `npm run test:e2e:visual`

예:

- 로그인/로그아웃 수정
- 장바구니 로직 수정
- 마이페이지 저장 동작 수정

### 9-3. 디자인을 의도적으로 바꾼 경우

추천 순서:

1. `npm run test:e2e:visual`
2. 실패 화면이 의도한 변경인지 확인
3. 맞다면 `npm run test:e2e:visual:update`
4. 다시 `npm run test:e2e:visual`

즉 update는 항상 "검토 후" 사용하는 게 안전합니다.

---

## 10. 자주 쓰는 명령어 정리

```bash
# 개발 서버 실행
npm run dev

# 배포 빌드 검증
npm run build

# Vitest 1회 실행
npm test

# Vitest watch 모드
npm run test:watch

# 기능 E2E 실행
npm run test:e2e

# 기능 E2E를 브라우저 창 보이게 실행
npm run test:e2e:headed

# 시각 회귀 테스트 실행
npm run test:e2e:visual

# 시각 회귀 baseline 갱신
npm run test:e2e:visual:update
```

---

## 11. 한 줄씩만 다시 요약하면

- `build`는 "배포 가능한 코드 상태인가"를 본다.
- `Vitest`는 "컴포넌트와 함수가 기대대로 반응하는가"를 본다.
- `기능 E2E`는 "브라우저에서 사용자 흐름이 실제로 이어지는가"를 본다.
- `시각 회귀`는 "화면 모양이 예전과 비교해 깨지지 않았는가"를 본다.

이 프로젝트는 이미 이 4단계를 나눠서 갖추고 있기 때문에,  
앞으로는 수정 범위에 맞게 적절한 명령어를 골라 돌리면 됩니다.

작은 로직 수정이면 `build + test`,  
사용자 흐름이 바뀌면 `build + test + e2e`,  
레이아웃이나 반응형을 건드렸다면 `visual`까지 같이 보는 식으로 이해하면 가장 실무적으로 맞습니다.
