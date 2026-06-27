from __future__ import annotations

import argparse
import json
import mimetypes
import uuid
from dataclasses import dataclass
from http import cookiejar
from pathlib import Path
from typing import Any
from urllib import error, parse, request


DEFAULT_BASE_URL = "http://localhost:8080"
TARGET_COUNT_PER_CATEGORY = 10
ALLOWED_IMAGE_HOSTS = {"images.pexels.com"}
MAX_IMAGE_DOWNLOAD_BYTES = 8 * 1024 * 1024


@dataclass(frozen=True)
class ProductSeed:
    name: str
    price: int
    quantity: int
    image_url: str


class ApiClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.cookies = cookiejar.CookieJar()
        self.opener = request.build_opener(request.HTTPCookieProcessor(self.cookies))

    def _send(
        self,
        method: str,
        path: str,
        *,
        data: bytes | None = None,
        headers: dict[str, str] | None = None,
        expect_json: bool = True,
    ) -> Any:
        req = request.Request(
            url=f"{self.base_url}{path}",
            method=method,
            data=data,
            headers=headers or {},
        )

        try:
            with self.opener.open(req) as response:
                raw = response.read().decode("utf-8")
        except error.HTTPError as exc:
            raw = exc.read().decode("utf-8")
            raise RuntimeError(f"{method} {path} failed with {exc.code}: {raw}") from exc

        if not raw:
            return None
        if expect_json:
            return json.loads(raw)
        return raw

    def login(self, email: str, password: str) -> dict[str, Any]:
        payload = json.dumps({"email": email, "password": password}).encode("utf-8")
        return self._send(
            "POST",
            "/api/login",
            data=payload,
            headers={"Content-Type": "application/json"},
        )

    def fetch_categories(self) -> list[dict[str, Any]]:
        return self._send("GET", "/api/categories")

    def fetch_items(self) -> list[dict[str, Any]]:
        return self._send("GET", "/api/items")

    def create_category(self, name: str) -> dict[str, Any]:
        payload = json.dumps({"name": name}).encode("utf-8")
        return self._send(
            "POST",
            "/api/categories",
            data=payload,
            headers={"Content-Type": "application/json"},
        )

    def create_item(
        self,
        *,
        item_name: str,
        price: int,
        quantity: int,
        category_id: int,
        image_name: str,
        image_bytes: bytes,
        image_content_type: str,
    ) -> dict[str, Any]:
        body, content_type = encode_multipart_formdata(
            fields={
                "itemName": item_name,
                "price": str(price),
                "quantity": str(quantity),
                "categoryId": str(category_id),
            },
            file_field_name="imageFile",
            file_name=image_name,
            file_bytes=image_bytes,
            file_content_type=image_content_type,
        )
        return self._send(
            "POST",
            "/api/items",
            data=body,
            headers={"Content-Type": content_type},
        )


def encode_multipart_formdata(
    *,
    fields: dict[str, str],
    file_field_name: str,
    file_name: str,
    file_bytes: bytes,
    file_content_type: str,
) -> tuple[bytes, str]:
    boundary = f"----CodexBoundary{uuid.uuid4().hex}"
    lines: list[bytes] = []

    for key, value in fields.items():
        lines.append(f"--{boundary}".encode("utf-8"))
        lines.append(
            f'Content-Disposition: form-data; name="{key}"'.encode("utf-8")
        )
        lines.append(b"")
        lines.append(value.encode("utf-8"))

    lines.append(f"--{boundary}".encode("utf-8"))
    lines.append(
        (
            f'Content-Disposition: form-data; name="{file_field_name}"; '
            f'filename="{file_name}"'
        ).encode("utf-8")
    )
    lines.append(f"Content-Type: {file_content_type}".encode("utf-8"))
    lines.append(b"")
    lines.append(file_bytes)
    lines.append(f"--{boundary}--".encode("utf-8"))
    lines.append(b"")

    body = b"\r\n".join(lines)
    return body, f"multipart/form-data; boundary={boundary}"


def download_image(image_url: str) -> tuple[str, bytes, str]:
    parsed_url = parse.urlparse(image_url)
    if parsed_url.scheme != "https" or parsed_url.hostname not in ALLOWED_IMAGE_HOSTS:
        raise RuntimeError(
            f"허용되지 않은 이미지 URL입니다: {image_url} "
            f"(allowed hosts: {', '.join(sorted(ALLOWED_IMAGE_HOSTS))})"
        )

    req = request.Request(
        image_url,
        headers={
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/123.0.0.0 Safari/537.36"
            )
        },
    )
    with request.urlopen(req) as response:
        content_type = response.headers.get_content_type() or "image/jpeg"
        if not content_type.startswith("image/"):
            raise RuntimeError(f"이미지 응답이 아닙니다: {image_url} ({content_type})")

        image_bytes = response.read(MAX_IMAGE_DOWNLOAD_BYTES + 1)

    if len(image_bytes) > MAX_IMAGE_DOWNLOAD_BYTES:
        raise RuntimeError(f"이미지 파일이 너무 큽니다: {image_url}")

    extension = mimetypes.guess_extension(content_type) or ".jpg"
    file_name = f"seed-{uuid.uuid4().hex}{extension}"
    return file_name, image_bytes, content_type


def group_items_by_category(items: list[dict[str, Any]]) -> dict[int, list[dict[str, Any]]]:
    grouped: dict[int, list[dict[str, Any]]] = {}
    for item in items:
        category_id = item.get("categoryId")
        if category_id is None:
            continue
        grouped.setdefault(int(category_id), []).append(item)
    return grouped


CATEGORY_PLANS: dict[str, list[ProductSeed]] = {
    "상의": [
        ProductSeed("스톤 워시 티셔츠", 36000, 18, "https://images.pexels.com/photos/9775681/pexels-photo-9775681.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("미니멀 테리 후디", 69000, 12, "https://images.pexels.com/photos/35408208/pexels-photo-35408208.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("소프트 블루 니트", 64000, 14, "https://images.pexels.com/photos/14463985/pexels-photo-14463985.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("캐주얼 코튼 셔츠", 52000, 16, "https://images.pexels.com/photos/17042021/pexels-photo-17042021.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("데일리 스트라이프 셔츠", 54000, 11, "https://images.pexels.com/photos/34262675/pexels-photo-34262675.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("에센셜 오버핏 반팔", 33000, 22, "https://images.pexels.com/photos/30368895/pexels-photo-30368895.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("크림 베이직 스웨트셔츠", 59000, 9, "https://images.pexels.com/photos/20584519/pexels-photo-20584519.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("슬림 헨리넥 티셔츠", 41000, 15, "https://images.pexels.com/photos/33258835/pexels-photo-33258835.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("라이트 코튼 롱슬리브", 45000, 13, "https://images.pexels.com/photos/9775681/pexels-photo-9775681.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("모던 하프집 니트", 72000, 8, "https://images.pexels.com/photos/14463985/pexels-photo-14463985.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "하의": [
        ProductSeed("클래식 스트레이트 진", 62000, 17, "https://images.pexels.com/photos/4109798/pexels-photo-4109798.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("미드 블루 와이드 데님", 69000, 13, "https://images.pexels.com/photos/33706493/pexels-photo-33706493.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("어반 카고 데님", 74000, 10, "https://images.pexels.com/photos/29191569/pexels-photo-29191569.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("라이트 워시 데님 팬츠", 59000, 15, "https://images.pexels.com/photos/35568704/pexels-photo-35568704.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("베이지 치노 팬츠", 68000, 12, "https://images.pexels.com/photos/34939558/pexels-photo-34939558.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("딥 인디고 진", 71000, 8, "https://images.pexels.com/photos/1598507/pexels-photo-1598507.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("슬림 테이퍼드 팬츠", 66000, 14, "https://images.pexels.com/photos/4109798/pexels-photo-4109798.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("빈티지 워시 진", 73000, 9, "https://images.pexels.com/photos/33706493/pexels-photo-33706493.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("커브드 핏 데님", 76000, 11, "https://images.pexels.com/photos/29191569/pexels-photo-29191569.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("이지 코튼 트라우저", 65000, 19, "https://images.pexels.com/photos/35568704/pexels-photo-35568704.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
    "아우터": [
        ProductSeed("시티 레더 재킷", 149000, 7, "https://images.pexels.com/photos/12148300/pexels-photo-12148300.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("샌드 트렌치 코트", 179000, 5, "https://images.pexels.com/photos/9968540/pexels-photo-9968540.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("클래식 블레이저 재킷", 129000, 8, "https://images.pexels.com/photos/8343180/pexels-photo-8343180.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("모던 버튼 코트", 189000, 4, "https://images.pexels.com/photos/19487834/pexels-photo-19487834.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("스웨이드 집업 재킷", 159000, 6, "https://images.pexels.com/photos/7667440/pexels-photo-7667440.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("드레이프 롱 코트", 169000, 5, "https://images.pexels.com/photos/35892521/pexels-photo-35892521.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("포멀 네이비 블레이저", 139000, 9, "https://images.pexels.com/photos/8343180/pexels-photo-8343180.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("라이트 필드 재킷", 119000, 10, "https://images.pexels.com/photos/12148300/pexels-photo-12148300.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("테일러드 싱글 코트", 199000, 3, "https://images.pexels.com/photos/19487834/pexels-photo-19487834.jpeg?auto=compress&cs=tinysrgb&w=1200"),
        ProductSeed("데일리 집업 블루종", 129000, 7, "https://images.pexels.com/photos/7667440/pexels-photo-7667440.jpeg?auto=compress&cs=tinysrgb&w=1200"),
    ],
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "관리자 계정으로 로그인한 뒤 실제 /api/items 를 호출해 "
            "상의/하의/아우터 카테고리를 각 10개씩 맞추는 시드 스크립트입니다."
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
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    client = ApiClient(args.base_url)
    client.login(args.admin_email, args.admin_password)

    categories = client.fetch_categories()
    existing_category_names = {str(category["name"]) for category in categories}
    for category_name in CATEGORY_PLANS:
        if category_name not in existing_category_names:
            created_category = client.create_category(category_name)
            categories.append(created_category)
            print(
                f"[CATEGORY] created '{created_category['name']}' "
                f"(id={created_category['id']})"
            )

    items = client.fetch_items()
    items_by_category = group_items_by_category(items)

    category_id_by_name: dict[str, int] = {
        str(category["name"]): int(category["id"]) for category in categories
    }

    created_items: list[dict[str, Any]] = []

    for category_name, plan in CATEGORY_PLANS.items():
        if category_name not in category_id_by_name:
            raise RuntimeError(f"카테고리 '{category_name}' 를 찾을 수 없습니다.")

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
