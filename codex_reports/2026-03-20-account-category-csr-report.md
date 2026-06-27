# 2026-03-20 Account / Category / CSR 안정화 보고서

## 1. 이번 작업 요약

- 프론트에서 API 응답이 비어 있거나 shape가 깨졌을 때 하얀 화면으로 이어질 수 있는 경로를 막았습니다.
- 홈 화면에 카테고리 대표이미지 섹션을 만들고, 클릭 시 백엔드 `GET /api/items?categoryId=` 필터를 그대로 사용하도록 연결했습니다.
- `/account` 진입 시 과거 LazyInitializationException 이 왜 났는지 프론트와 백엔드 흐름을 같이 확인했습니다.

## 2. 구현 내용

### 2-1. CSR 흰 화면 방지

변경 파일:

- `shopping_fe/src/api/client.ts`
- `shopping_fe/src/components/AppErrorBoundary.tsx`
- `shopping_fe/src/main.tsx`

핵심 변경:

- `client.ts`에서 배열/객체 응답을 정규화하도록 보강했습니다.
- `200 OK`인데 body가 비어 있거나, 예상한 JSON shape가 아니면 제어된 `Error`로 바꾸도록 했습니다.
- `fetchMyPage`, `fetchItems`, `fetchSession`, `fetchPosts`, `fetchCategories` 등에서 바로 `null`이 화면으로 흘러가지 않게 막았습니다.
- 최종 안전망으로 `AppErrorBoundary`를 추가해서, 혹시 놓친 렌더 예외가 나도 전체 화면이 하얗게 사라지지 않고 복구 UI를 보여주도록 했습니다.

### 2-2. 카테고리 대표이미지 섹션

변경 파일:

- `shopping_fe/src/pages/HomePage.tsx`
- `shopping_fe/src/styles/app.css`

핵심 변경:

- 홈 화면 카테고리 블록을 대표이미지 카드로 변경했습니다.
- 카드 이미지는 `CategoryDTO.representativeImageUrl` 값을 사용합니다.
- 카드를 누르면 기존처럼 `fetchItems({ categoryId })`로 다시 조회합니다.
- 선택된 카테고리를 상단 카테고리 섹션에서 바로 확인할 수 있게 정리했습니다.
- 기존 텍스트-only 카드보다 실제 탐색 카드처럼 보이도록 스타일을 보강했습니다.

## 3. `/account` 진입 시 과거 Lazy Exception 원인

결론부터 말하면, **`AccountPage.tsx`가 화면에서 `cart.item.category`를 직접 읽어서 터진 것은 아닙니다.**

### 3-1. 프론트에서 실제로 일어난 호출

`/account`로 들어가면 로그인 상태 기준으로 최소 2개의 흐름이 있습니다.

1. `shopping_fe/src/contexts/CartContext.tsx`
   - `user?.id`가 있으면 전역 `CartProvider`가 `fetchCart()`를 호출합니다.
   - 이 호출은 `/account` 페이지 전용이 아니라, 로그인된 모든 페이지에서 공통으로 동작합니다.

2. `shopping_fe/src/pages/AccountPage.tsx`
   - `useEffect` 안에서 `fetchMyPage()`를 호출합니다.
   - 이 호출은 백엔드 `/api/myPage`를 가져옵니다.

즉 `/account` 진입 시점에는 `GET /api/cart` 와 `GET /api/myPage` 둘 다 문제 후보였습니다.

### 3-2. `AccountPage.tsx` 자체는 category를 직접 렌더하지 않음

`shopping_fe/src/pages/AccountPage.tsx`를 보면 장바구니 영역에서 쓰는 값은 사실상 아래 수준입니다.

- `entry.itemName`
- `entry.price`

화면 코드만 보면 category를 직접 꺼내는 부분은 없습니다.

### 3-3. 그런데도 category lazy loading이 터진 이유

원인은 **백엔드가 JSON 응답용 DTO를 만드는 과정에서 category를 읽었기 때문**입니다.

관련 백엔드 흐름:

1. `shopping_be/src/main/java/myex/shopping/controller/api/ApiUserController.java`
   - `/api/myPage`에서 `Cart cart = cartService.findOrCreateCartForUser(loginUser);`
   - 그 뒤 `new MyPageDto(loginUser, orders, posts, cart)` 생성

2. `shopping_be/src/main/java/myex/shopping/dto/mypagedto/MyPageDto.java`
   - `cart.getCartItems().stream().map(oi -> new ItemDto(oi.getItem()))`

3. `shopping_be/src/main/java/myex/shopping/dto/itemdto/ItemDto.java`
   - `item.getCategory() != null ? item.getCategory().getId() : null`
   - `item.getCategory() != null ? item.getCategory().getName() : null`

즉, 프론트가 category를 안 써도 백엔드는 응답 DTO 생성 과정에서 이미 `item.getCategory()`를 접근합니다.

### 3-4. 왜 fetch join 미비가 바로 예외로 이어졌는가

`Item.category`는 `LAZY` 입니다.

- `shopping_be/src/main/java/myex/shopping/domain/Item.java`
  - `@ManyToOne(fetch = FetchType.LAZY)`

과거에 cart 조회가 `cart -> cartItems -> item`까지만 fetch join 되고 `item.category`는 fetch join 되지 않았다면:

- 트랜잭션/영속성 컨텍스트 밖에서
- `new ItemDto(oi.getItem())`
- 내부에서 `item.getCategory().getName()`

이 순간 `LazyInitializationException` 이 터질 수 있습니다.

즉 핵심 원인은:

- 프론트의 화면 렌더 코드가 아니라
- **백엔드 DTO 생성 시점의 LAZY 연관 접근**
- 그리고 **cart 조회 쿼리에서 `i.category`를 fetch join 하지 않았던 것**

입니다.

### 3-5. `/api/cart`도 같은 위험이 있었음

이 문제는 `/api/myPage`만의 문제가 아니었습니다.

`shopping_be/src/main/java/myex/shopping/controller/api/ApiCartController.java`

- `new CartDto(cart)`

`shopping_be/src/main/java/myex/shopping/dto/cartdto/CartDto.java`

- `cart.getCartItems().stream().map(CartItemDto::new)`

`shopping_be/src/main/java/myex/shopping/dto/cartitemdto/CartItemDto.java`

- `this.item = new ItemDto(cartItem.getItem())`

결국 `/api/cart`도 최종적으로 `ItemDto`를 만들기 때문에, `item.category`를 fetch join 하지 않으면 같은 계열의 예외가 날 수 있었습니다.

따라서 `/account` 진입 시에는:

- 전역 `fetchCart()`
- 페이지 전용 `fetchMyPage()`

두 경로 모두 잠재적으로 같은 category lazy 문제를 일으킬 수 있었습니다.

## 4. 현재 기준에서 왜 이제 괜찮은가

현재 `JpaCartRepository`는 cart 조회 시 category까지 fetch join 하도록 되어 있습니다.

- `shopping_be/src/main/java/myex/shopping/repository/jpa/JpaCartRepository.java`
  - `left join fetch ci.item i`
  - `left join fetch i.category`

그리고 이 부분은 테스트로도 고정되어 있습니다.

- `shopping_be/src/test/java/myex/shopping/controller/api/ApiCartAndMyPageOpenInViewDisabledTest.java`
  - `open-in-view=false` 환경에서 `/api/cart`와 `/api/myPage`가 categoryName을 반환하는지 검증

따라서 지금 기준 원인 정리는 다음 한 줄로 요약할 수 있습니다.

> `AccountPage.tsx`가 category를 직접 읽어서 터진 것이 아니라, `/account` 진입 시 호출된 백엔드 cart/myPage API가 DTO를 만드는 과정에서 `item.category`를 접근했고, 당시 cart 조회 쿼리가 category를 fetch join 하지 않아 LazyInitializationException 이 발생했을 가능성이 높다.

## 5. 검증

프론트 실행 확인:

- `npm test`
- `npm run build`

결과:

- Vitest 전체 통과
- Vite production build 통과

## 6. 참고 변경 파일

- `shopping_fe/src/api/client.ts`
- `shopping_fe/src/components/AppErrorBoundary.tsx`
- `shopping_fe/src/main.tsx`
- `shopping_fe/src/pages/HomePage.tsx`
- `shopping_fe/src/styles/app.css`
- `shopping_fe/src/test/api-client-auth.test.ts`
- `shopping_fe/src/test/home-page.test.tsx`
