# `style.css` 린터 오류 수정

작성일: 2026-03-11

대상 파일:

- `shopping_be/src/main/resources/static/style.css`

## 수정 내용

CSS 파일 안에 들어가 있던 HTML 주석을 전부 CSS 표준 주석으로 변경했습니다.

변경 전:

```css
<!-- 장바구니 관련-->
```

변경 후:

```css
/* 장바구니 관련 */
```

같은 방식으로 아래 섹션 주석도 모두 수정했습니다.

- 장바구니 관련
- 마이페이지 관련
- Post 게시판 관련 css
- posts/new 게시판 등록 관련
- posts/view.html : 상세 게시물 보기
- items 관련
- items.html
- item.html 관련
- addForm.html 관련
- editForm.html 관련

## 왜 이게 문제였나

`<!-- ... -->` 는 HTML 주석 문법입니다.

CSS 파일 안에서는 표준 주석이 아니기 때문에 IDE/CSS 린터가 아래처럼 볼 수 있습니다.

- 잘못된 토큰
- 알 수 없는 셀렉터
- 구문 오류

브라우저나 일부 CSS 파서는 관대하게 넘어가기도 하지만, 린터는 보통 엄격하게 잡습니다.

## 확인 결과

수정 후 `style.css` 안에 `<!--` 패턴은 더 이상 없습니다.

## 참고

현재 파일에는 중복 셀렉터와 들여쓰기 불균형은 남아 있습니다. 이건 보통 "문법 오류"는 아니고, 사용하는 린트 규칙에 따라 추가 경고가 될 수 있습니다.

만약 IDE에서 아직 빨간줄이 남아 있으면, 다음 중 하나일 가능성이 큽니다.

- duplicate selector 경고
- formatting/indentation 규칙
- 특정 CSS inspection 규칙

그 경우 정확한 에러 메시지 한 줄만 주면 바로 이어서 맞춰서 정리할 수 있습니다.
