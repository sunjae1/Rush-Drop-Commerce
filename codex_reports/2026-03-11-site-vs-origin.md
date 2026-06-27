# `same-site`와 `same-origin` 차이

작성일: 2026-03-11

질문:

```text
https://shop.example.com
https://api.example.com

이게 왜 같은 사이트야?
```

## 결론

브라우저 표준에서:

- `same-origin`은 아닙니다
- `same-site`는 맞습니다

즉:

- CORS 기준으로는 서로 다른 origin
- `SameSite` 쿠키 기준으로는 같은 site

이 둘은 기준이 다릅니다.

## 1. `origin` 기준

`origin`은 아래 3개가 모두 같아야 같습니다.

- scheme
- host
- port

예시 비교:

- `https://shop.example.com`
- `https://api.example.com`

비교 결과:

- scheme: 둘 다 `https` -> 같음
- host: `shop.example.com` vs `api.example.com` -> 다름
- port: 둘 다 기본 443 -> 같다고 봐도 됨

따라서 host가 다르므로 `same-origin`이 아닙니다.

즉 브라우저는 이 둘을 cross-origin으로 봅니다. 그래서 CORS가 필요합니다.

## 2. `site` 기준

브라우저의 `site`는 보통 아래 기준으로 봅니다.

- scheme
- registrable domain

여기서 `registrable domain`은 대충 "실제로 등록 가능한 기본 도메인"이라고 보면 됩니다.

예:

- `shop.example.com`의 registrable domain -> `example.com`
- `api.example.com`의 registrable domain -> `example.com`

둘 다:

- scheme: `https`
- registrable domain: `example.com`

이므로 `same-site`입니다.

즉:

- `shop`은 서브도메인
- `api`도 서브도메인
- 핵심 기준은 둘 다 `example.com` 아래라는 점입니다

## 3. 왜 이렇게 기준이 둘로 나뉘나

브라우저가 보는 목적이 다르기 때문입니다.

### CORS

CORS는 "어느 출처(origin)가 저 서버에 직접 요청해도 되나?"를 보는 규칙입니다.

그래서 매우 엄격하게:

- scheme
- host
- port

를 전부 봅니다.

### SameSite 쿠키

SameSite는 "이 요청이 대체로 같은 사이트 맥락에서 온 건가?"를 보는 규칙입니다.

그래서 서브도메인이 달라도:

- `shop.example.com`
- `api.example.com`

은 같은 site로 묶을 수 있습니다.

## 4. 네 예시를 표로 보면

| URL 1 | URL 2 | same-origin? | same-site? | 이유 |
|---|---|---|---|---|
| `https://shop.example.com` | `https://api.example.com` | 아니오 | 예 | host는 다르지만 scheme + registrable domain이 같음 |
| `https://shop.example.com` | `https://shop.example.com:8443` | 아니오 | 예 | port가 달라 origin은 다름 |
| `http://shop.example.com` | `https://api.example.com` | 아니오 | 아니오 | scheme이 달라서 modern browser 기준 same-site도 아님 |
| `https://shop.example.com` | `https://api.other.com` | 아니오 | 아니오 | registrable domain이 다름 |

## 5. 네 프로젝트에 대입하면

### 경우 1

- 프론트: `https://shop.example.com`
- 백엔드: `https://api.example.com`

이 경우:

- CORS 필요
- 하지만 쿠키 관점에서는 same-site로 볼 가능성이 큼

즉 CORS는 반드시 설정해야 하지만, 완전히 다른 도메인끼리 붙는 경우보다 쿠키 정책은 덜 까다로울 수 있습니다.

### 경우 2

- 프론트: `https://myshop.com`
- 백엔드: `https://api.other.com`

이 경우:

- CORS 필요
- same-site도 아님
- 세션 쿠키까지 더 엄격하게 맞춰야 함

보통 이때는:

- `SameSite=None`
- `Secure=true`
- `credentials: "include"`
- 정확한 `Access-Control-Allow-Origin`
- `Access-Control-Allow-Credentials: true`

가 같이 필요합니다.

## 6. 한 줄 정리

`shop.example.com`과 `api.example.com`은:

- 서로 다른 origin이라서 CORS 대상이고
- 같은 site라서 쿠키 `SameSite` 관점에서는 같은 사이트 계열로 볼 수 있습니다

둘은 모순이 아니라 기준이 다른 것입니다.
