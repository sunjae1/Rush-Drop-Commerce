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
