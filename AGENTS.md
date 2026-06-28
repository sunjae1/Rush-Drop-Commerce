# Workspace Rules

## Report Location

- 사용자가 보고서, 계획서, 로드맵, 체크리스트, 리뷰 정리처럼 Markdown 문서를 요청하면 결과 파일은 **반드시 프로젝트 루트의 `codex_reports/advancement/` 폴더에만** 저장합니다.
- 보고서 파일명은 `000_설명.md` 형식의 세 자리 숫자 prefix를 붙이고, 기존 파일 중 가장 큰 번호 다음 번호를 사용해 오름차순으로 쌓습니다. 예: `006_mock-payment-stage1-report.md`.
- 이런 문서는 `shopping_fe/docs`, `Md_report`, 각 서비스 하위 폴더 등 다른 위치에 새로 만들지 않습니다. 사용자가 특정 다른 경로를 명시한 경우만 예외로 합니다.
- 이 규칙은 서브 에이전트를 쓰는 경우와 쓰지 않는 경우 모두 동일하게 적용합니다.

## Database Migration Policy

- Flyway migration에는 스키마 변경(DDL)과 정말 필요한 기준 데이터만 둡니다.
- 상품, 주문, 게시글, 데모 이미지 URL처럼 개발/포트폴리오용 샘플 데이터(DML)는 **절대** Flyway migration에 넣지 않습니다.
- 샘플 데이터는 프로젝트 루트의 `scripts/` 시드 스크립트나 프론트 데모 seed에서 관리합니다.
- 이미 적용된 Flyway migration 파일은 checksum 문제가 생기므로 수정하거나 삭제하지 않습니다. 잘못 적용된 seed 성격의 migration은 새 migration으로 데이터를 정리하거나, 사용자의 명시 요청이 있을 때만 `flyway_schema_history`를 별도로 보정합니다.

## Backend Agent Manuals

- `shopping_be` 백엔드 작업을 할 때는 작업 성격에 맞는 `shopping_be/agents/` 문서를 먼저 읽고 따릅니다.
- 테스트 작성, 검증, 버그 재현, 회귀 확인 작업이면 반드시 `shopping_be/agents/tester.md`를 먼저 읽습니다.
- 구현 작업이면 `shopping_be/agents/developer.md`를 먼저 읽습니다.
- 원인 분석, 장애 분석, 디버깅, 코드 리뷰 작업이면 `shopping_be/agents/analyzer.md`를 먼저 읽습니다.
- 기획, 단계 분리, 로드맵, 작업 순서 설계가 필요하면 `shopping_be/agents/planner.md`를 먼저 읽습니다.
- 하나의 작업에 여러 성격이 섞이면 관련 문서를 모두 읽습니다.
- 하위 agent 문서와 이 루트 `AGENTS.md`가 충돌하면 루트 `AGENTS.md`를 우선합니다. 특히 보고서 저장 위치는 항상 `codex_reports/advancement/` 규칙을 따릅니다.

## Backend Verification Harness

- `shopping_be`에서 결제, 주문, 장바구니, 재고, 인증, 보안 설정, Flyway migration을 수정하면 컨트롤러 테스트만으로 완료하지 않습니다.
- 최소 검증 범위는 관련 Service 테스트, 관련 Controller/API 테스트, 필요한 Repository 또는 migration 검증 테스트입니다.
- DB 스키마, 제약조건, Flyway migration을 수정하면 테스트 환경에서 migration SQL 또는 그 효과를 검증하는 테스트를 추가하거나 실행합니다.
- 결제, 주문, 장바구니, 재고처럼 상태 전이가 있는 기능은 성공 케이스뿐 아니라 실패, 금액 불일치, 권한 오류, 재고 복구 같은 edge case를 검증합니다.
- 백엔드 작업 완료 전에는 가능한 한 `shopping_be`에서 `.\gradlew.bat test`를 실행합니다. 실행하지 못한 경우 최종 답변에 이유와 남은 리스크를 명시합니다.
- 프론트와 백엔드가 함께 바뀌는 작업은 백엔드 테스트와 프론트 테스트를 모두 실행하고, 통합 흐름이 깨질 수 있는 경우 E2E 또는 수동 검증 결과를 함께 보고합니다.
