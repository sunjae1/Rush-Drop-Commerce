# 쇼핑몰 프로젝트

Spring Boot로 제작된 쇼핑몰 웹 애플리케이션입니다.

** 현재 비용 문제로 AWS 를 Terminate 한 상태 입니다. **

## AWS 웹서버 배포 URL: https://5jkdcnqok36jucnd3a34xorpqa0mcsky.lambda-url.ap-northeast-2.on.aws/
-   **참고:** AWS Lambda + Cloud Watch 를 사용하여, 비용 절감을 위해 첫 요청 시 서버를 키는데 시간이 걸릴 수 있습니다.
## 주요 기능

-   **사용자 관리:** 회원가입 및 로그인 기능
-   **상품 관리:** 상품 조회 및 수정, 삭제 기능
-   **장바구니:** 장바구니에 상품을 담는 기능
-   **주문 시스템:** 장바구니에 담긴 상품을 주문하는 기능
-   **커뮤니티:** 사용자를 위한 간단한 게시판 기능
-   **REST API:** 모든 기능에 대한 RESTful API를 제공하며, Swagger로 문서화 제공.

## 사용 기술

-   **프레임워크:** Spring Boot
-   **언어:** Java 21
-   **템플릿 엔진:** Thymeleaf
-   **데이터베이스:** MySQL
-   **API 문서:** Springdoc OpenAPI (Swagger)
-   **빌드 도구:** Gradle

## 실행 방법

1.  저장소를 복제합니다.
2.  Gradle wrapper를 사용하여 애플리케이션을 실행합니다: `./gradlew bootRun`
3.  `http://localhost:8080`에서 애플리케이션에 접속할 수 있습니다.
4.  Swagger API 문서는 `http://localhost:8080/swagger-ui.html`에서 확인할 수 있습니다.

## CI/CD 및 배포 전략

이 프로젝트는 **GitHub Actions**를 활용하여 CI/CD 파이프라인을 구축하고, AWS EC2 서버에 무중단 배포를 수행합니다.

-   **CI/CD:** GitHub Actions Work-Flow를 사용하여, 코드가 push될 때마다 자동으로 빌드, 배포가 트리거 됩니다.
-   **서버 호스팅:**
    -  메인 웹 애플리케이션을 **AWS EC2**인스턴스에 배포합니다.
    -  AWS Lambda + Cloud Watch 를 활용해 요청이 없을 시 무서버(Serverless) 방식으로 실행하여 운영 비용을 최소화 합니다.


## 향후 계획

-   **인증/인가 강화:** Spring Security를 도입하여 보다 안전한 인증 및 인가 기능을 구현할 예정입니다.
-   **결제 기능 구현:** 테스트 결제 모듈을 연동하여 실제와 유사한 주문 및 결제 프로세스를 구현할 계획입니다.
-   **검색 기능 구현:** QueryDSL을 사용하여 동적 검색 기능 구현 및 향후 고도화 검색 필요시 Elasticsearch 도입 검토할 예정입니다.
