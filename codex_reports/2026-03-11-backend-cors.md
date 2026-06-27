# 백엔드 CORS 설정 및 동작 설명

작성일: 2026-03-11

앞으로 작업 보고서는 `codex_reports/` 아래에 Markdown으로 남깁니다.

## 1. `VITE_API_BASE_URL`이 없으면 왜 `/api`로 호출되는가

프론트 코드:

```ts
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

function buildUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}
```

핵심은 "`없으면 공백`"이 맞지만, 그 다음 줄에서 문자열 결합이 일어난다는 점입니다.

- `VITE_API_BASE_URL`이 없으면 `API_BASE_URL = ""`
- `buildUrl("/api/items")`를 실행하면 결과는 `"" + "/api/items"`라서 `"/api/items"`
- 즉 "빈 문자열로 호출"이 아니라 "현재 브라우저 origin 기준의 상대경로"로 호출됩니다

예를 들면:

- 프론트가 `http://localhost:5173`에서 열려 있으면 브라우저가 실제로 요청하는 주소는 `http://localhost:5173/api/items`
- 프론트가 `https://shop.example.com`에서 열려 있으면 실제 요청 주소는 `https://shop.example.com/api/items`

관련 파일:

- `shopping_fe/src/api/client.ts`

## 2. Vite 프록시 덕분에 왜 개발환경에서 CORS 없이 되는가

프론트 개발 서버 설정:

```ts
server: {
  proxy: {
    "/api": {
      target: proxyTarget,
      changeOrigin: true
    }
  }
}
```

동작 순서는 아래와 같습니다.

1. 브라우저는 `http://localhost:5173/api/items`로 요청합니다
2. 브라우저 입장에서는 "현재 페이지와 같은 origin(5173)"으로 요청한 것입니다
3. 그래서 브라우저는 이 단계에서 CORS를 검사하지 않습니다
4. Vite 개발 서버가 그 요청을 받아서 뒤에서 `http://localhost:8080/api/items`로 전달합니다
5. 이 5173 -> 8080 전달은 브라우저가 아니라 개발 서버가 하는 일이므로 브라우저 CORS 대상이 아닙니다

즉, CORS를 피한 것이 아니라 "브라우저가 cross-origin 요청을 직접 하지 않도록 프록시가 대신 받아주는 구조"입니다.

관련 파일:

- `shopping_fe/vite.config.ts`

## 3. `credentials: "include"`는 무엇을 뜻하는가

프론트 요청 코드:

```ts
fetch(url, {
  credentials: "include"
})
```

이 옵션은 "쿠키를 같이 보내고, 응답의 쿠키도 받을 수 있게 해 달라"는 뜻입니다.

세션 로그인 기반 백엔드에서는 이 옵션이 중요합니다. 현재 백엔드는 `JSESSIONID` 쿠키 기반 세션을 사용합니다.

### 3-1. 개발환경에서 프록시를 쓸 때

- 브라우저는 `localhost:5173`과만 통신합니다
- 쿠키도 브라우저 입장에서는 `5173` 응답에서 받은 것으로 저장됩니다
- 이후 `/api/...` 요청 때 그 쿠키를 같이 보내고
- Vite 프록시가 그 쿠키를 백엔드 `8080`으로 전달합니다

그래서 개발환경에서는 세션 로그인도 꽤 자연스럽게 동작합니다.

### 3-2. 프론트가 백엔드를 직접 다른 origin으로 호출할 때

예:

- 프론트: `http://localhost:5173`
- 백엔드: `http://localhost:8080`

이 경우 브라우저는 cross-origin 요청으로 판단합니다. 그래서 백엔드는 아래 응답을 맞춰야 합니다.

- `Access-Control-Allow-Origin: http://localhost:5173`
- `Access-Control-Allow-Credentials: true`

여기서 중요한 점:

- `credentials: "include"`를 쓰면 `Access-Control-Allow-Origin: *`는 사용할 수 없습니다
- 반드시 정확한 origin을 돌려줘야 합니다

### 3-3. 쿠키 `SameSite` / `Secure` 이슈는 언제 생기나

이 부분은 "다른 origin"과 "다른 site"를 구분해야 합니다.

#### 같은 site지만 다른 origin인 경우

예:

- `https://shop.example.com`
- `https://api.example.com`

이 둘은 origin은 다르지만 보통 same-site로 취급됩니다. 이 경우에도 CORS는 필요합니다. 다만 쿠키 `SameSite` 이슈는 상대적으로 덜 까다롭습니다.

#### 완전히 다른 site인 경우

예:

- `https://myshop.com`
- `https://api-another-domain.com`

이 경우는 진짜 cross-site라서 세션 쿠키가 XHR/fetch에 실리려면 대체로 아래 조건이 필요합니다.

- 쿠키 `SameSite=None`
- 쿠키 `Secure=true` (HTTPS 필요)
- 프론트 `credentials: "include"`
- 백엔드 `Access-Control-Allow-Credentials: true`
- 백엔드 `Access-Control-Allow-Origin: 프론트 주소`

즉, 이번 작업은 CORS는 해결하지만, 프론트/백엔드가 완전히 다른 사이트로 분리 배포된다면 쿠키 정책은 별도로 추가 확인이 필요합니다.

관련 파일:

- `shopping_fe/src/api/client.ts`
- `shopping_be/src/main/java/myex/shopping/config/SecurityConfig.java`

## 4. 이번에 적용한 변경

### 백엔드 CORS 설정 추가

파일:

- `shopping_be/src/main/java/myex/shopping/config/SecurityConfig.java`

변경 내용:

- `/api/**` 보안 체인에 `cors()` 활성화
- `CorsConfigurationSource` Bean 추가
- 허용 origin을 `FRONT_END_ORIGIN` 환경변수에서 읽도록 설정
- `allowCredentials(true)` 적용
- 허용 메서드: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`

### 환경변수 매핑 추가

파일:

- `shopping_be/src/main/resources/application.yml`

추가 내용:

```yaml
app:
  cors:
    allowed-origin: ${FRONT_END_ORIGIN:http://localhost:5173}
```

의미:

- `FRONT_END_ORIGIN`이 있으면 그 값을 사용
- 없으면 기본값 `http://localhost:5173`

### 예시 env 파일 갱신

파일:

- `shopping_be/.env.example`

추가 내용:

```env
FRONT_END_ORIGIN=http://localhost:5173
```

### 테스트 추가

파일:

- `shopping_be/src/test/java/myex/shopping/config/SecurityConfigIntegrationTest.java`

추가한 검증:

- 허용된 origin으로 `/api/items` preflight 요청 시
- `Access-Control-Allow-Origin`
- `Access-Control-Allow-Credentials`

헤더가 정상적으로 내려오는지 확인

## 5. 사용 방법

로컬에서 프론트가 `http://localhost:5173`이면 기본값 그대로도 동작합니다.

프론트를 다른 주소에서 띄우면 백엔드 환경변수에 정확한 프론트 주소를 넣으면 됩니다.

예:

```env
FRONT_END_ORIGIN=https://shop.example.com
```

주의:

- 프로토콜까지 포함해야 합니다
- `*`는 `credentials: "include"`와 함께 쓰면 안 됩니다
- 포트가 다르면 다른 origin입니다

## 6. 검증 결과

실행 명령:

```powershell
.\gradlew.bat test --tests myex.shopping.config.SecurityConfigIntegrationTest
```

결과:

- `BUILD SUCCESSFUL`

## 7. 남은 주의 사항

현재 반영한 것은 CORS까지입니다.

만약 최종 배포 구조가 아래처럼 완전히 다른 사이트라면:

- 프론트 `https://myshop.com`
- 백엔드 `https://api-other.com`

추가로 세션 쿠키 설정도 봐야 합니다.

- `server.servlet.session.cookie.same-site=None`
- `server.servlet.session.cookie.secure=true`

이 부분이 필요하면 다음 요청에서 바로 이어서 추가할 수 있습니다.
