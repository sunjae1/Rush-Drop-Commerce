from __future__ import annotations

import argparse
from typing import Any

from seed_real_products import (
    ApiClient,
    DEFAULT_BASE_URL,
    TARGET_COUNT_PER_CATEGORY,
    ProductSeed,
    download_image,
    group_items_by_category,
)


CATEGORY_PLANS: dict[str, list[ProductSeed]] = {
    "신발": [
        ProductSeed("레드 러너 스니커즈 드롭", 89000, 6, "https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("라이트 코트 스니커즈", 79000, 12, "https://images.pexels.com/photos/15398044/pexels-photo-15398044.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("파스텔 러닝 스니커즈", 99000, 8, "https://images.pexels.com/photos/29548615/pexels-photo-29548615.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("크로스워크 캔버스 슈즈", 69000, 13, "https://images.pexels.com/photos/3375910/pexels-photo-3375910.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("블랙 하이탑 스니커즈", 86000, 9, "https://images.pexels.com/photos/9333372/pexels-photo-9333372.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("컬러 콘크리트 스니커즈", 92000, 7, "https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("데일리 화이트 스니커즈", 76000, 14, "https://images.pexels.com/photos/15398044/pexels-photo-15398044.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("어반 퍼플 러너", 97000, 8, "https://images.pexels.com/photos/29548615/pexels-photo-29548615.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("모노 캔버스 슈즈", 72000, 11, "https://images.pexels.com/photos/9333372/pexels-photo-9333372.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("시티 페어 스니커즈", 84000, 10, "https://images.pexels.com/photos/3375910/pexels-photo-3375910.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "가방": [
        ProductSeed("클래식 레더 토트백", 129000, 8, "https://images.pexels.com/photos/29096395/pexels-photo-29096395.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("미니 체인 숄더백", 99000, 10, "https://images.pexels.com/photos/34997535/pexels-photo-34997535.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("에브리데이 캔버스 숄더백", 69000, 15, "https://images.pexels.com/photos/34027219/pexels-photo-34027219.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("시티 버클 크로스백", 89000, 11, "https://images.pexels.com/photos/29793778/pexels-photo-29793778.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("소프트 호보백", 119000, 7, "https://images.pexels.com/photos/36364966/pexels-photo-36364966.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("슬림 메신저백", 109000, 9, "https://images.pexels.com/photos/36492563/pexels-photo-36492563.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("골드 이브닝 백", 139000, 6, "https://images.pexels.com/photos/34965306/pexels-photo-34965306.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("트래블 위켄더 백", 149000, 5, "https://images.pexels.com/photos/29096395/pexels-photo-29096395.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("베이지 데일리 숄더백", 96000, 12, "https://images.pexels.com/photos/34027219/pexels-photo-34027219.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("스트럭처 미니 백", 93000, 10, "https://images.pexels.com/photos/36364966/pexels-photo-36364966.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "모자": [
        ProductSeed("옐로우 볼캡 선착순 드롭", 39000, 10, "https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("블루 로고 볼캡", 42000, 12, "https://images.pexels.com/photos/13697753/pexels-photo-13697753.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("스토어 그래픽 캡", 45000, 9, "https://images.pexels.com/photos/31162881/pexels-photo-31162881.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("퍼플 부티크 캡", 41000, 11, "https://images.pexels.com/photos/18313293/pexels-photo-18313293.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("컬러 셸프 볼캡", 43000, 13, "https://images.pexels.com/photos/13697753/pexels-photo-13697753.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("레드 포인트 캡", 39000, 14, "https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("블랙 트러커 캡", 46000, 8, "https://images.pexels.com/photos/31162881/pexels-photo-31162881.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("라벤더 데일리 캡", 40000, 12, "https://images.pexels.com/photos/18313293/pexels-photo-18313293.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("그린 플랜트 볼캡", 44000, 9, "https://images.pexels.com/photos/13697753/pexels-photo-13697753.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("선글라스 세트 볼캡", 49000, 7, "https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "시계": [
        ProductSeed("헤리티지 브라운 스트랩 워치", 189000, 7, "https://images.pexels.com/photos/9395501/pexels-photo-9395501.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("클래식 골드 다이얼 워치", 219000, 5, "https://images.pexels.com/photos/11780439/pexels-photo-11780439.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("스톤 클래식 레더 워치", 199000, 8, "https://images.pexels.com/photos/34031837/pexels-photo-34031837.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("포레스트 브라운 워치", 179000, 9, "https://images.pexels.com/photos/9381643/pexels-photo-9381643.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("블랙 크로노 워치", 249000, 4, "https://images.pexels.com/photos/3083461/pexels-photo-3083461.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("로즈 핑크 레더 워치", 169000, 10, "https://images.pexels.com/photos/11780439/pexels-photo-11780439.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("두바이 클래식 워치", 239000, 6, "https://images.pexels.com/photos/10347090/pexels-photo-10347090.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("문페이즈 드레스 워치", 279000, 3, "https://images.pexels.com/photos/13548994/pexels-photo-13548994.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("다크 클래식 워치", 189000, 7, "https://images.pexels.com/photos/6349111/pexels-photo-6349111.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("아웃도어 블랙 스트랩 워치", 209000, 8, "https://images.pexels.com/photos/28958280/pexels-photo-28958280.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "머플러": [
        ProductSeed("클래식 페이즐리 머플러", 49000, 14, "https://images.pexels.com/photos/34108352/pexels-photo-34108352.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("소프트 플로럴 스카프", 45000, 16, "https://images.pexels.com/photos/36211083/pexels-photo-36211083.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("윈터 컬러 블렌드 머플러", 52000, 12, "https://images.pexels.com/photos/30327445/pexels-photo-30327445.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("시그니처 포인트 스카프", 43000, 13, "https://images.pexels.com/photos/32721785/pexels-photo-32721785.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("데일리 블루 머플러", 39000, 18, "https://images.pexels.com/photos/30886909/pexels-photo-30886909.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("모던 헤드스카프", 47000, 11, "https://images.pexels.com/photos/34225149/pexels-photo-34225149.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("브리즈 플라워 스카프", 44000, 15, "https://images.pexels.com/photos/7720485/pexels-photo-7720485.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("핑크 블룸 머플러", 41000, 12, "https://images.pexels.com/photos/36211083/pexels-photo-36211083.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("컬러 포인트 니트 머플러", 53000, 9, "https://images.pexels.com/photos/30327445/pexels-photo-30327445.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("데일리 패턴 스카프", 42000, 14, "https://images.pexels.com/photos/34108352/pexels-photo-34108352.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "액세서리": [
        ProductSeed("럭셔리 링 세트", 79000, 10, "https://images.pexels.com/photos/29986281/pexels-photo-29986281.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("빈티지 펄 네크리스", 89000, 7, "https://images.pexels.com/photos/36229132/pexels-photo-36229132.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("터콰이즈 비즈 브레이슬릿", 45000, 16, "https://images.pexels.com/photos/10596294/pexels-photo-10596294.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("골드 이어링 포인트", 68000, 11, "https://images.pexels.com/photos/32989031/pexels-photo-32989031.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("비즈 레이어드 네크리스", 59000, 13, "https://images.pexels.com/photos/20257348/pexels-photo-20257348.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("사파이어 무드 이어링", 72000, 9, "https://images.pexels.com/photos/29193423/pexels-photo-29193423.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("골드 젬 브레이슬릿", 95000, 6, "https://images.pexels.com/photos/29245551/pexels-photo-29245551.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("마켓 빈티지 주얼리 믹스", 52000, 18, "https://images.pexels.com/photos/5602433/pexels-photo-5602433.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("레진 팬던트 네크리스", 47000, 14, "https://images.pexels.com/photos/17225139/pexels-photo-17225139.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("오너먼트 주얼리 세트", 99000, 8, "https://images.pexels.com/photos/18016511/pexels-photo-18016511.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "벨트": [
        ProductSeed("웨이브 패턴 레더 벨트", 59000, 12, "https://images.pexels.com/photos/32498751/pexels-photo-32498751.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("오너먼트 브라운 벨트", 69000, 10, "https://images.pexels.com/photos/35392702/pexels-photo-35392702.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("실버 버클 블랙 벨트", 72000, 9, "https://images.pexels.com/photos/35392722/pexels-photo-35392722.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("웨스턴 스터드 벨트", 84000, 7, "https://images.pexels.com/photos/32498611/pexels-photo-32498611.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("클래식 수트 벨트", 65000, 11, "https://images.pexels.com/photos/35392702/pexels-photo-35392702.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("미니멀 데님 벨트", 54000, 15, "https://images.pexels.com/photos/32498751/pexels-photo-32498751.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("드레스 라인 벨트", 68000, 8, "https://images.pexels.com/photos/35392722/pexels-photo-35392722.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("브라운 소가죽 벨트", 61000, 13, "https://images.pexels.com/photos/35392702/pexels-photo-35392702.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("스퀘어 버클 벨트", 56000, 14, "https://images.pexels.com/photos/32498751/pexels-photo-32498751.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("포인트 버클 벨트", 63000, 10, "https://images.pexels.com/photos/32498611/pexels-photo-32498611.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "선글라스": [
        ProductSeed("메탈 프레임 선글라스", 79000, 14, "https://images.pexels.com/photos/32677205/pexels-photo-32677205.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("레트로 라운드 선글라스", 69000, 13, "https://images.pexels.com/photos/701877/pexels-photo-701877.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("스퀘어 그린 선글라스", 72000, 12, "https://images.pexels.com/photos/15848006/pexels-photo-15848006.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("로즈 골드 선글라스", 76000, 10, "https://images.pexels.com/photos/9957406/pexels-photo-9957406.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("시티 블랙 선글라스", 74000, 11, "https://images.pexels.com/photos/30120606/pexels-photo-30120606.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("선셋 브라운 선글라스", 70000, 14, "https://images.pexels.com/photos/23279646/pexels-photo-23279646.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("체인 디테일 선글라스", 85000, 8, "https://images.pexels.com/photos/10220085/pexels-photo-10220085.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("모노크롬 스타일 선글라스", 73000, 9, "https://images.pexels.com/photos/33769219/pexels-photo-33769219.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("클래식 시티 선글라스", 71000, 12, "https://images.pexels.com/photos/17373985/pexels-photo-17373985.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("데일리 틴트 선글라스", 68000, 15, "https://images.pexels.com/photos/24800137/pexels-photo-24800137.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "키즈": [
        ProductSeed("키즈 데일리 후디 세트", 59000, 14, "https://images.pexels.com/photos/28808342/pexels-photo-28808342.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 라이트 윈드브레이커", 69000, 10, "https://images.pexels.com/photos/34608858/pexels-photo-34608858.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 컬러 선글라스 티셔츠", 39000, 18, "https://images.pexels.com/photos/35338045/pexels-photo-35338045.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 스포츠 저지 세트", 52000, 16, "https://images.pexels.com/photos/35338044/pexels-photo-35338044.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 파스텔 원피스", 64000, 9, "https://images.pexels.com/photos/14622860/pexels-photo-14622860.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 소프트 맨투맨", 43000, 17, "https://images.pexels.com/photos/5560018/pexels-photo-5560018.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 스트리트 점퍼", 71000, 8, "https://images.pexels.com/photos/33572819/pexels-photo-33572819.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 플레이웨어 세트", 47000, 15, "https://images.pexels.com/photos/30110229/pexels-photo-30110229.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 위켄드 팬츠", 41000, 19, "https://images.pexels.com/photos/34181925/pexels-photo-34181925.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("키즈 패턴 셔츠", 45000, 13, "https://images.pexels.com/photos/32486768/pexels-photo-32486768.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "신발/가방/모자/시계/머플러/액세서리/벨트/선글라스/키즈 카테고리에 대해 "
            "카테고리당 10개씩 실제 /api/items 를 통해 상품을 추가하는 스크립트입니다."
        )
    )
    parser.add_argument("--admin-email", required=True, help="관리자 이메일")
    parser.add_argument("--admin-password", required=True, help="관리자 비밀번호")
    parser.add_argument(
        "--base-url",
        default=DEFAULT_BASE_URL,
        help=f"API base URL (default: {DEFAULT_BASE_URL})",
    )
    parser.add_argument(
        "--target-count",
        type=int,
        default=TARGET_COUNT_PER_CATEGORY,
        help=f"카테고리별 목표 상품 수 (default: {TARGET_COUNT_PER_CATEGORY})",
    )
    parser.add_argument(
        "--create-missing-categories",
        action="store_true",
        help="카테고리가 없으면 자동 생성합니다. 기본값은 미생성 상태에서 실패입니다.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    client = ApiClient(args.base_url)
    client.login(args.admin_email, args.admin_password)

    categories = client.fetch_categories()
    category_id_by_name: dict[str, int] = {
        str(category["name"]): int(category["id"]) for category in categories
    }

    missing_categories = [
        category_name
        for category_name in CATEGORY_PLANS
        if category_name not in category_id_by_name
    ]

    if missing_categories and not args.create_missing_categories:
        raise RuntimeError(
            "다음 카테고리가 아직 없습니다: "
            + ", ".join(missing_categories)
            + " | 필요하면 --create-missing-categories 옵션을 사용하세요."
        )

    if missing_categories and args.create_missing_categories:
        for category_name in missing_categories:
            created_category = client.create_category(category_name)
            category_id_by_name[category_name] = int(created_category["id"])
            print(
                f"[CATEGORY] created '{created_category['name']}' "
                f"(id={created_category['id']})"
            )

    items = client.fetch_items()
    items_by_category = group_items_by_category(items)
    created_items: list[dict[str, Any]] = []

    for category_name, plan in CATEGORY_PLANS.items():
        category_id = category_id_by_name[category_name]
        existing_items = items_by_category.get(category_id, [])
        existing_names = {str(item["itemName"]) for item in existing_items}
        current_count = len(existing_items)

        print(
            f"[CATEGORY] {category_name} "
            f"current={current_count} target={args.target_count}"
        )

        for product in plan:
            if current_count >= args.target_count:
                break
            if product.name in existing_names:
                continue

            image_name, image_bytes, content_type = download_image(product.image_url)
            created = client.create_item(
                item_name=product.name,
                price=product.price,
                quantity=product.quantity,
                category_id=category_id,
                image_name=image_name,
                image_bytes=image_bytes,
                image_content_type=content_type,
            )
            created_items.append(created)
            current_count += 1
            existing_names.add(product.name)
            print(f"  + created #{created['id']} {created['itemName']}")

        if current_count < args.target_count:
            raise RuntimeError(
                f"카테고리 '{category_name}' 를 {args.target_count}개까지 채우지 못했습니다. "
                f"현재 {current_count}개입니다."
            )

    print("")
    print("=== SUMMARY ===")
    if not created_items:
        print("생성된 상품이 없습니다. 모든 카테고리가 이미 목표 수량을 만족합니다.")
        return

    for item in created_items:
        print(
            f"#{item['id']} {item['itemName']} "
            f"({item.get('categoryName')}, {item['price']}원)"
        )


if __name__ == "__main__":
    main()
