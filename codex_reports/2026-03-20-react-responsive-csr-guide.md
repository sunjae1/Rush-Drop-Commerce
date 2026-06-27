# React CSR 반응형 웹 구현 가이드

## 1. 한 줄 답변

React에서 반응형은 보통 `React 자체 기능`으로 만드는 것이 아니라, **React 컴포넌트 + CSS 반응형 기법** 또는 **UI 라이브러리의 breakpoint 기능**으로 만듭니다.

즉, 핵심은 아래 셋 중 하나입니다.

- CSS `@media`로 화면 크기에 따라 스타일 바꾸기
- Tailwind CSS처럼 breakpoint prefix로 클래스 바꾸기
- MUI 같은 UI 라이브러리에서 `sx`, `useMediaQuery`, breakpoint API 쓰기

## 2. "보통 @ 이런 식으로 어노테이션 넣는 거야?"에 대한 답

결론부터 말하면, **Java의 `@Controller`, `@Entity` 같은 어노테이션 개념은 아닙니다.**

React 반응형에서 많이 보는 `@` 문법은 거의 항상 CSS의 `@media` 입니다.

예:

```css
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

@media (max-width: 1080px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
```

이 `@media` 는 "화면이 이 너비보다 작아지면 스타일을 이렇게 바꿔라"라는 뜻입니다.

즉:

- React JSX 안에 어노테이션을 붙이는 방식이 아님
- 보통은 CSS 파일이나 CSS-in-JS 안에서 breakpoint를 정의함
- 어떤 라이브러리를 쓰느냐에 따라 문법만 달라짐

## 3. "따로 페이지를 만드는 건가?"에 대한 답

보통은 **페이지를 따로 만들지 않습니다.**

가장 흔한 방식은 아래와 같습니다.

1. 라우트는 그대로 유지한다.
2. 같은 페이지 컴포넌트 안에서 레이아웃만 화면 크기에 맞게 바꾼다.
3. 필요하면 일부 컴포넌트만 모바일/데스크톱 버전으로 나눈다.

예를 들면 `/products` 라는 같은 페이지에서:

- PC: 상품 4열 그리드
- 태블릿: 상품 2열 그리드
- 모바일: 상품 1열 + 필터를 드로어로 변경

이런 식으로 보통 처리합니다.

다만 아래 경우에는 분리할 수 있습니다.

- 모바일 UX가 데스크톱과 구조적으로 너무 다를 때
- 주문/결제 플로우를 모바일 전용으로 단순화할 때
- 관리자 화면처럼 데스크톱 중심 화면이 따로 필요할 때

그래도 **완전히 다른 페이지를 새로 만드는 것보다, 같은 route 안에서 컴포넌트 조합을 다르게 하는 쪽이 일반적**입니다.

## 4. React 반응형은 실제로 어떤 식으로 동작하나

### 4-1. 가장 기본: CSS `@media`

이 방식이 가장 보편적이고, 현재 프로젝트에도 가장 자연스럽습니다.

동작 방식:

- React는 평소처럼 JSX를 렌더링
- CSS가 화면 너비를 보고 레이아웃을 바꿈
- 같은 컴포넌트라도 모바일/PC에서 다르게 보임

예시:

```tsx
export function ProductGridSection() {
  return (
    <section className="catalog-section">
      <h2>이번 주 상품</h2>
      <div className="product-grid">
        <article className="product-card">상품 A</article>
        <article className="product-card">상품 B</article>
        <article className="product-card">상품 C</article>
        <article className="product-card">상품 D</article>
      </div>
    </section>
  );
}
```

```css
.catalog-section {
  padding: 32px 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

.product-card {
  min-height: 220px;
  border-radius: 20px;
  padding: 20px;
  background: #f6f1ea;
}

@media (max-width: 1080px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
```

특징:

- 가장 단순함
- CSS 이해가 쉬우면 유지보수가 편함
- 별도 라이브러리 의존성이 거의 없음

### 4-2. Tailwind CSS 방식

Tailwind는 CSS를 직접 많이 쓰기보다, JSX 안에 breakpoint 클래스를 적는 방식입니다.

예시:

```tsx
export function ProductGridSection() {
  return (
    <section className="px-4 py-8">
      <h2 className="mb-4 text-2xl font-semibold">이번 주 상품</h2>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <article className="min-h-[220px] rounded-2xl bg-stone-100 p-5">상품 A</article>
        <article className="min-h-[220px] rounded-2xl bg-stone-100 p-5">상품 B</article>
        <article className="min-h-[220px] rounded-2xl bg-stone-100 p-5">상품 C</article>
        <article className="min-h-[220px] rounded-2xl bg-stone-100 p-5">상품 D</article>
      </div>
    </section>
  );
}
```

해석:

- `grid-cols-1`: 기본은 1열
- `sm:grid-cols-2`: 작은 화면 이상에서는 2열
- `lg:grid-cols-4`: 큰 화면 이상에서는 4열

특징:

- 빠르게 화면을 만들기 좋음
- JSX에 클래스가 길어질 수 있음
- 디자이너/개발자 스타일 합의가 필요함

### 4-3. MUI(Material UI) 방식

MUI는 컴포넌트 라이브러리라서, 반응형도 JS/TS 안에서 같이 다루기 쉽습니다.

예시:

```tsx
import { Box, Card, Typography } from "@mui/material";

export function ProductGridSection() {
  return (
    <Box sx={{ px: 2, py: 4 }}>
      <Typography variant="h4" sx={{ mb: 2 }}>
        이번 주 상품
      </Typography>

      <Box
        sx={{
          display: "grid",
          gap: 2,
          gridTemplateColumns: {
            xs: "1fr",
            sm: "repeat(2, minmax(0, 1fr))",
            lg: "repeat(4, minmax(0, 1fr))"
          }
        }}
      >
        <Card sx={{ minHeight: 220, borderRadius: 4, p: 2 }}>상품 A</Card>
        <Card sx={{ minHeight: 220, borderRadius: 4, p: 2 }}>상품 B</Card>
        <Card sx={{ minHeight: 220, borderRadius: 4, p: 2 }}>상품 C</Card>
        <Card sx={{ minHeight: 220, borderRadius: 4, p: 2 }}>상품 D</Card>
      </Box>
    </Box>
  );
}
```

추가로 화면 크기에 따라 렌더링 자체를 나눌 수도 있습니다.

```tsx
import { useTheme } from "@mui/material/styles";
import useMediaQuery from "@mui/material/useMediaQuery";

export function HeaderActions() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));

  return isMobile ? <button>메뉴</button> : <nav>상단 네비게이션</nav>;
}
```

특징:

- 디자인 시스템이 필요한 팀에 유리함
- 컴포넌트가 풍부함
- 대신 의존성이 커지고, 기본 Material 느낌을 커스터마이징해야 할 수 있음

## 5. 라이브러리별 선택 기준

### 5-1. 순수 CSS / CSS Modules / Sass

추천 상황:

- 구조를 직접 통제하고 싶을 때
- 디자인이 커스텀 중심일 때
- 의존성을 늘리고 싶지 않을 때

장점:

- 가장 표준적임
- 성능/구조를 예측하기 쉬움
- 라이브러리 종속이 약함

주의:

- 클래스 설계가 엉키면 CSS 관리가 어려워질 수 있음

### 5-2. Tailwind CSS

추천 상황:

- 빠르게 화면을 많이 만들어야 할 때
- 디자인 토큰과 규칙을 팀 단위로 통일할 때
- 반복되는 CSS 작성을 줄이고 싶을 때

장점:

- 개발 속도가 빠름
- breakpoint 사용이 간단함
- 유틸리티 조합이 쉬움

주의:

- JSX className이 길어질 수 있음
- 초기에 팀이 문법에 익숙해져야 함

### 5-3. MUI

추천 상황:

- 관리자 화면, 폼, 테이블, 다이얼로그가 많을 때
- 완성도 높은 UI 컴포넌트를 빨리 쓰고 싶을 때
- 디자인 시스템과 breakpoint API를 같이 관리하고 싶을 때

장점:

- 컴포넌트 품질이 안정적임
- 반응형 API가 체계적임
- 폼/테이블/모달 구축이 빠름

주의:

- 번들 및 의존성 부담이 늘어남
- 디자인이 모두 비슷해 보이지 않게 손봐야 할 수 있음

## 6. 현재 `shopping_fe` 프로젝트 기준 추천

현재 프론트는 아래 조합입니다.

- Vite 5
- React 18
- TypeScript
- React Router 6
- 전역 CSS 파일 사용

그리고 실제 코드에서도 이미 `shopping_fe/src/styles/app.css` 안에 `@media (max-width: 1080px)`, `@media (max-width: 760px)` 방식이 들어가 있습니다.

즉, **이 프로젝트는 이미 "CSS `@media` 기반 반응형" 방향으로 가고 있습니다.**

그래서 가장 현실적인 추천은 아래입니다.

1. 지금은 `app.css` 또는 CSS Modules 기준으로 반응형을 계속 확장한다.
2. 레이아웃은 `grid`, `flex`, `minmax()`, `clamp()` 중심으로 잡는다.
3. 모바일 우선으로 breakpoints를 정리한다.
4. 정말 속도가 필요하거나 디자인 시스템을 통일해야 할 때만 Tailwind 또는 MUI를 도입한다.

개인적으로 이 저장소에는 아래 우선순위를 추천합니다.

1. 현재 방식 유지: CSS `@media`
2. 스타일 충돌이 커지면 CSS Modules 도입
3. 대규모 UI 재구성이 필요하면 Tailwind 또는 MUI 중 하나만 선택

`Tailwind` 와 `MUI` 를 동시에 강하게 섞는 것은 보통 관리 포인트가 늘어서 추천하지 않습니다.

## 7. 실무에서 보통 어떻게 구성하나

실무에서 가장 흔한 패턴은 아래와 같습니다.

### 패턴 A. 같은 페이지 + CSS만 변경

예:

- `HomePage.tsx` 는 그대로 둠
- `.hero`, `.product-grid`, `.site-nav` 만 breakpoint에 따라 변경

이 방식이 가장 일반적입니다.

### 패턴 B. 같은 페이지 + 일부 컴포넌트 분기

예:

```tsx
export function Header() {
  const isMobile = window.innerWidth < 760;

  return isMobile ? <MobileHeader /> : <DesktopHeader />;
}
```

실무에서는 보통 `window.innerWidth`를 직접 쓰기보다 `useMediaQuery` 또는 커스텀 훅을 씁니다.

이 방식은 아래에 적합합니다.

- 모바일 메뉴가 햄버거 드로어일 때
- PC만 사이드바를 보여줄 때
- 모바일에서 필터 UI를 모달/드로어로 바꿔야 할 때

### 패턴 C. 페이지는 같고 레이아웃 래퍼만 분기

예:

- `/account` route는 하나
- 내부에서 모바일은 세로 카드
- 데스크톱은 2열 대시보드

이 방식도 많이 씁니다.

## 8. 반응형 구현 체크리스트

- 고정 width보다 `max-width`, `minmax()`, `%`, `clamp()`를 우선 사용
- 그리드는 `grid-template-columns`로 단계별 제어
- 네비게이션은 모바일에서 가로 스크롤, 드로어, 토글 메뉴 중 하나로 전환
- 버튼, 입력창, 카드 여백을 화면 크기별로 줄이기
- 이미지에 `width: 100%`, `height: auto` 기본 적용
- 모바일에서 hover 의존 UI를 피하기
- 실제 브라우저 DevTools 디바이스 모드와 실기기에서 같이 확인

## 9. 결론

질문하신 내용을 짧게 정리하면 아래와 같습니다.

- React 반응형은 보통 `@media`, Tailwind breakpoint, MUI breakpoint API로 만든다.
- Java 같은 어노테이션을 다는 방식은 아니다.
- 보통은 페이지를 따로 만들지 않고, **같은 페이지에서 레이아웃과 컴포넌트만 반응형으로 바꾼다.**
- 현재 `shopping_fe` 프로젝트에는 이미 CSS `@media` 기반 구조가 있으므로, 그 방향으로 확장하는 것이 가장 자연스럽다.

필요하면 다음 단계로 바로 이어서 아래 문서도 만들 수 있습니다.

- `shopping_fe` 기준 반응형 레이아웃 설계안
- 홈/상품상세/장바구니 반응형 와이어프레임 초안
- 실제 적용용 CSS breakpoint 설계표
