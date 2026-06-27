# 004 Product Detail Image Model Report

작성일: 2026-06-26

## 목표

상품 상세 페이지가 추천 상품 이미지를 임시로 재활용하는 수준을 넘어서, 상품마다 별도 상세 이미지를 가질 수 있도록 데이터 모델, API 응답, 프론트 렌더링, 개발용 seed 스크립트를 추가했다.

## 구현 요약

백엔드:

- `ItemDetailImage` 엔티티 추가
- `ItemDetailImageRole` enum 추가: `MOOD`, `DETAIL`
- `Item`과 `ItemDetailImage`를 1:N 관계로 연결
- `ItemDetailImageDto` 추가
- `GET /api/items/{id}` 응답의 `ItemDto.detailImages` 추가
- `ImageService`가 상세 이미지 URL도 S3 key 또는 외부 URL 규칙에 맞게 resolve하도록 확장

DB:

- `V7__create_item_detail_image_table.sql` 추가
- DML 없이 DDL만 포함
- `item_detail_image` 테이블 생성
- `(item_id, display_order)` unique 제약 추가
- `image_role` check 제약 추가

프론트:

- `ItemDetailImage` 타입 추가
- API normalizer에서 `detailImages` 배열 파싱
- 상품 상세 페이지의 착용/무드 컷과 소재/디테일 이미지는 `item.detailImages`를 우선 사용
- 상세 이미지가 없으면 기존처럼 같은 카테고리 추천 상품 이미지로 fallback

스크립트:

- `scripts/seed_item_detail_images.py` 추가
- `.env`의 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 읽어 dev DB에 접속
- 활성 상품마다 상세 이미지 3장 생성
  - `MOOD` 2장
  - `DETAIL` 1장
- 기본 실행은 상세 이미지가 없는 상품만 채움
- `--replace` 옵션으로 대상 상품의 상세 이미지를 삭제 후 재생성 가능
- `--dry-run`, `--item-id`, `--limit` 옵션 지원

## 데이터 모델

테이블: `item_detail_image`

| 컬럼 | 역할 |
| --- | --- |
| `id` | 상세 이미지 ID |
| `item_id` | 상품 FK |
| `display_order` | 상세 페이지 노출 순서 |
| `image_role` | `MOOD` 또는 `DETAIL` |
| `image_url` | 이미지 URL 또는 S3 key |
| `alt_text` | 접근성용 대체 텍스트 |
| `caption` | 화면 캡션 |

이 구조를 둔 이유:

- 상품 대표 이미지는 `item.image_url` 그대로 유지
- 상세 페이지용 이미지는 별도 테이블로 분리
- 한 상품에 여러 상세 이미지를 순서 있게 붙일 수 있음
- 긴 JPG 하나가 아니라 이미지 역할별 데이터로 관리 가능
- 프론트에서 반응형/접근성/SEO 친화적인 HTML 상세 페이지를 구성 가능

## 실행 결과

Flyway:

- `V7 create item detail image table` 적용 완료
- `flyway_schema_history`에 version `7` 성공 기록 확인

Seed:

```bash
py scripts/seed_item_detail_images.py --dry-run
py scripts/seed_item_detail_images.py
```

결과:

- 활성 상품: 91개
- 생성 상세 이미지: 273행
- 역할별 개수: `MOOD` 182개, `DETAIL` 91개
- 상품별 상세 이미지 수: 최소 3개, 최대 3개

API 확인:

- 임시 백엔드 포트 `18080`에서 새 코드로 확인
- `GET /api/items/1`
- 응답 결과: `detailImages=3`, 첫 이미지 역할 `MOOD`
- 확인 후 임시 프로세스 종료

## 테스트

실행 명령:

```bash
cd shopping_be
.\gradlew.bat test --tests myex.shopping.controller.api.ApiItemControllerTest
.\gradlew.bat test

cd ../shopping_fe
npm test -- src/test/product-page.test.tsx
npm run build
npm test
```

결과:

- 백엔드 상품 API 테스트 통과
- 백엔드 전체 테스트 통과
- 프론트 상품 상세 테스트 통과
- 프론트 production build 통과
- 프론트 전체 Vitest: 14 files, 43 tests passed

참고:

- 프론트 테스트에서 기존 React Router future flag 경고와 일부 테스트 라우트 경고는 출력됐지만 실패는 없었다.
- `python` 명령은 PATH에 없고, 이 환경에서는 Windows Python launcher인 `py`를 사용해야 한다.

## 사용법

전체 상품 중 상세 이미지가 아직 없는 상품만 채우기:

```bash
py scripts/seed_item_detail_images.py
```

실제로 쓰기 전에 몇 행이 생성될지 확인:

```bash
py scripts/seed_item_detail_images.py --dry-run
```

특정 상품만 다시 채우기:

```bash
py scripts/seed_item_detail_images.py --item-id 1 --replace
```

처음 10개 상품만 확인:

```bash
py scripts/seed_item_detail_images.py --limit 10 --dry-run
```

## 남은 리스크

- 현재 seed 이미지는 외부 이미지 URL을 DB에 저장한다. 운영 품질로 가려면 상세 이미지 업로드 API나 관리자 UI를 추가해서 S3 key 기반으로 관리하는 편이 좋다.
- 현재 API는 단건 상품 조회에서만 상세 이미지를 채운다. 목록 API는 payload를 줄이기 위해 `detailImages: []`를 반환한다.
- 현재 관리자 상품 등록/수정 화면에서는 상세 이미지를 관리하지 않는다. 포트폴리오 다음 단계로는 상세 이미지 업로드/정렬 UI를 붙일 수 있다.
