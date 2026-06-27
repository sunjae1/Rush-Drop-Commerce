# Cross-Origin 배포 체크리스트

## 1. 현재 상태 요약

- 프론트 정적 파일은 `S3 + CloudFront`에서 정상 응답 중이다.
- `dist/assets/*.js`, `dist/assets/*.css`도 CloudFront에서 `200`으로 내려오고 있다.
- 현재 문제는 정적 파일이 아니라 백엔드 API 연결이다.
- 현재 프론트 번들에는 `http://ec2-3-39-147-146.ap-northeast-2.compute.amazonaws.com/` 가 박혀 있다.
- 현재 백엔드는:
  - `https` 443 포트가 열려 있지 않다.
  - CloudFront Origin 기준 CORS preflight가 `403 Invalid CORS request` 로 실패한다.

즉, 지금 우선순위는 `백엔드 HTTPS` 와 `백엔드 CORS` 다.

---

## 2. 목표 구조

Cross-origin 운영 목표는 아래처럼 잡으면 된다.

- 프론트: `https://d3ha4j5aabjw9j.cloudfront.net`
  - 또는 나중에 `https://www.example.com`
- 백엔드 API: `https://api.example.com`

브라우저 기준으로는:

- 프론트 origin: `https://d3ha4j5aabjw9j.cloudfront.net`
- API origin: `https://api.example.com`

이 둘은 host가 다르므로 cross-origin이다.  
그래서 반드시 아래 3개가 동시에 맞아야 한다.

1. 백엔드가 `https` 로 열려 있어야 함
2. 백엔드 CORS가 프론트 origin을 허용해야 함
3. 세션 쿠키를 쓸 거면 `SameSite=None`, `Secure` 설정이 필요함

---

## 3. 해야 할 일 순서

### 3-1. 전체 순서

1. 백엔드 API용 도메인 준비
2. 백엔드 `https` 구성
3. 백엔드 CORS 설정 반영
4. 세션 쿠키 설정 반영
5. 프론트 `.env.production` 수정
6. 프론트 재빌드
7. `dist`를 S3에 재업로드
8. CloudFront 캐시 무효화
9. 브라우저에서 실제 동작 확인

Cross-origin에서는 백엔드부터 끝내고 프론트를 다시 빌드하는 순서가 맞다.

---

## 4. 프론트 파트

### 4-1. 해야 할 일

1. `shopping_fe/.env.production` 에 운영 API 주소를 넣는다.
2. 이때 반드시 `https` 주소를 넣는다.
3. 끝의 `/` 는 빼는 것을 권장한다.
4. `npm run build` 로 다시 빌드한다.
5. 생성된 `dist` 전체를 S3에 업로드한다.
6. CloudFront invalidation을 수행한다.

### 4-2. 프론트 환경변수 예시

```dotenv
VITE_API_BASE_URL=https://api.example.com
```

권장하지 않는 예시:

```dotenv
VITE_API_BASE_URL=http://ec2-3-39-147-146.ap-northeast-2.compute.amazonaws.com/
```

이유:

- `http` 이면 CloudFront `https` 페이지에서 Mixed Content가 난다.
- EC2 기본 퍼블릭 도메인은 운영 API 도메인으로 쓰기 불편하다.
- 끝의 `/` 는 중복 슬래시 URL을 만들 수 있다.

### 4-3. 프론트 빌드

```powershell
cd shopping_fe
npm run build
```

### 4-4. 프론트 업로드 후 확인할 것

- S3에 `index.html`, `assets/*`, `brand-mark.svg` 가 모두 올라갔는지
- CloudFront 기본 Root Object가 `index.html` 인지
- CloudFront invalidation `/*` 를 했는지
- 브라우저 Network 탭에서 `/assets/*.js`, `/assets/*.css` 가 `200` 인지

---

## 5. 백엔드 파트

### 5-1. 해야 할 일

1. 운영 백엔드에 붙일 API 도메인을 정한다.
2. 그 도메인을 EC2로 연결한다.
3. Nginx에서 `443 https` 를 열고 Spring `8080` 으로 프록시한다.
4. 백엔드 운영 `.env` 에 `FRONT_END_ORIGIN` 을 정확히 넣는다.
5. 세션 쿠키 설정을 cross-origin 대응으로 바꾼다.
6. 백엔드 컨테이너 또는 앱을 재시작한다.
7. CORS preflight와 실제 API 호출을 확인한다.

### 5-2. 운영 `.env`

공백 없이 써야 한다.

```dotenv
FRONT_END_ORIGIN=https://d3ha4j5aabjw9j.cloudfront.net
```

주의:

- `FRONT_END_ORIGIN =...` 처럼 `=` 앞뒤 공백이 있으면 안 된다.
- 로컬 PC 파일만 수정해서는 안 되고, 실제 EC2 서버에서 실행 중인 컨테이너가 읽는 `.env` 를 수정해야 한다.

### 5-3. 세션 쿠키 설정

현재 프론트는 `credentials: "include"` 로 세션 쿠키를 보낸다.  
cross-origin에서 로그인/세션을 쓰려면 보통 아래 설정이 필요하다.

`shopping_be/src/main/resources/application-prod.yml`

```yaml
server:
  servlet:
    session:
      cookie:
        same-site: none
        secure: true
```

설명:

- `same-site: none`
  - 다른 origin 프론트에서 쿠키를 포함할 수 있게 함
- `secure: true`
  - `https` 에서만 쿠키 전송

### 5-4. CORS 설정 확인 포인트

현재 코드는 이미 아래 구조로 되어 있다.

- `application.yml`:
  - `app.cors.allowed-origin: ${FRONT_END_ORIGIN:http://localhost:5173}`
- `SecurityConfig.java`:
  - `setAllowCredentials(true)`
  - `setAllowedOrigins(List.of(frontEndOrigin))`

즉, 코드 수정 전에도 운영 `.env` 만 올바르게 주입되면 동작 가능하다.  
다만 운영 `.env` 값 반영 후 앱 재시작은 반드시 필요하다.

---

## 6. EC2에 HTTPS 도메인 붙이기

## 6-1. Route 53을 써야 하나?

필수는 아니다.  
Route 53은 `DNS 관리` 용이다.

즉:

- Route 53은 도메인을 어느 서버로 보낼지 정하는 역할
- HTTPS 인증서 자체를 발급해 주는 역할은 아님

현재 구조가 `EC2 + Nginx` 라면 가장 쉬운 방법은:

1. 도메인 준비
2. Route 53으로 DNS 연결
3. EC2에 Nginx 유지
4. EC2에서 Let’s Encrypt로 인증서 발급

이 방식이다.

### 6-2. 추천 방식 1: 현재 구조 유지

구조:

- `api.example.com` -> Route 53 -> EC2 Elastic IP
- Nginx:
  - `80` 수신
  - `443` 수신
  - `443` 에서 Spring `8080` 으로 `proxy_pass`

이 방식이 현재 상황에서 가장 적은 변경으로 끝난다.

### 6-3. 추천 방식 2: AWS 표준 방식

구조:

- `api.example.com` -> Route 53 -> ALB
- ACM 인증서 -> ALB에 연결
- ALB -> EC2 Target Group -> Spring

이 방식이 더 AWS 표준에 가깝지만, 지금은 Nginx가 이미 있으므로 바로 갈아탈 필요는 없다.

---

## 7. Route 53 + EC2 + Nginx 방식 상세 순서

### 7-1. 도메인 준비

예시:

- 프론트: `d3ha4j5aabjw9j.cloudfront.net`
- 백엔드 API: `api.example.com`

이미 도메인이 있다면 그 하위 도메인 `api.example.com` 을 쓰면 된다.

### 7-2. EC2에 Elastic IP 붙이기

이유:

- 퍼블릭 IP가 바뀌지 않게 고정해야 DNS를 안정적으로 연결할 수 있다.

### 7-3. Route 53 레코드 생성

Hosted Zone에서 예시:

- 레코드 이름: `api`
- 레코드 타입: `A`
- 값: EC2 Elastic IP

그러면:

- `api.example.com` -> EC2

### 7-4. 보안 그룹 확인

EC2 Security Group에서 최소한 아래 포트를 열어야 한다.

- `80`
- `443`

### 7-5. Nginx 설치 및 프록시 구성

현재처럼 Spring은 `8080`, Nginx는 앞단에서 받는다.

Nginx 예시:

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

먼저 HTTP로 붙는지 확인한다.

### 7-6. Let’s Encrypt 인증서 발급

Ubuntu 예시:

```bash
sudo apt update
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d api.example.com
```

이 작업이 끝나면 보통 Nginx가 자동으로 `443 ssl` 설정을 추가해 준다.

### 7-7. HTTPS 리다이렉트 확인

최종적으로는:

- `http://api.example.com` -> `https://api.example.com`
- `https://api.example.com/api/items` -> 정상 응답

이렇게 되어야 한다.

### 7-8. 검증 명령

```bash
curl -I https://api.example.com
curl -i https://api.example.com/api/items
curl -i -X OPTIONS \
  -H "Origin: https://d3ha4j5aabjw9j.cloudfront.net" \
  -H "Access-Control-Request-Method: GET" \
  https://api.example.com/api/items
```

마지막 preflight 응답에는 최소한 아래가 보여야 한다.

- `Access-Control-Allow-Origin: https://d3ha4j5aabjw9j.cloudfront.net`
- `Access-Control-Allow-Credentials: true`

---

## 8. CloudFront 파트

현재 확인 결과, 정적 파일 서빙 자체는 정상이다.  
그래도 운영 배포 체크리스트는 아래처럼 가져가면 된다.

### 8-1. 확인할 항목

- Origin:
  - S3 버킷이 연결되어 있는지
- Default root object:
  - `index.html`
- Custom error response:
  - `403 -> /index.html -> 200`
  - `404 -> /index.html -> 200`
- 배포 후 invalidation:
  - `/*`

### 8-2. 추가로 할 수 있는 것

프론트도 나중에 커스텀 도메인을 붙일 수 있다.

예:

- `www.example.com` -> Route 53 -> CloudFront

이 경우 CloudFront용 ACM 인증서는 `us-east-1` 에서 발급해야 한다.

---

## 9. 최종 점검 순서

### 9-1. 백엔드 먼저

1. `api.example.com` 준비
2. Route 53 -> EC2 Elastic IP 연결
3. Nginx + Let’s Encrypt로 `https://api.example.com` 완성
4. 운영 `.env` 의 `FRONT_END_ORIGIN` 수정
5. 쿠키 설정 반영
6. 앱 재시작
7. `curl` 로 CORS preflight 확인

### 9-2. 프론트 다음

1. `.env.production` 수정
2. `VITE_API_BASE_URL=https://api.example.com`
3. `npm run build`
4. `dist` 전체 S3 업로드
5. CloudFront invalidation

### 9-3. 브라우저 최종 확인

1. CloudFront 프론트 접속
2. 개발자도구 Console 확인
3. Network에서 `/assets/*.js` 가 `200` 인지 확인
4. Network에서 `https://api.example.com/api/items` 가 `200` 인지 확인
5. Response Headers에 CORS 헤더가 있는지 확인
6. 로그인 시 쿠키가 저장되는지 확인

---

## 10. 지금 바로 해야 하는 최소 작업

가장 먼저 할 일만 뽑으면 아래 순서다.

1. 백엔드 API용 실제 도메인 결정
2. EC2에 Elastic IP 연결
3. Route 53에서 `api.example.com` -> Elastic IP 연결
4. Nginx + Certbot으로 `https://api.example.com` 구성
5. 운영 백엔드 `.env` 에 아래 값 적용

```dotenv
FRONT_END_ORIGIN=https://d3ha4j5aabjw9j.cloudfront.net
```

6. `application-prod.yml` 에 세션 쿠키 설정 추가

```yaml
server:
  servlet:
    session:
      cookie:
        same-site: none
        secure: true
```

7. 백엔드 재시작
8. 프론트 `.env.production` 수정

```dotenv
VITE_API_BASE_URL=https://api.example.com
```

9. 프론트 재빌드
10. S3 재업로드 + CloudFront invalidation

이 순서대로 하면 된다.
