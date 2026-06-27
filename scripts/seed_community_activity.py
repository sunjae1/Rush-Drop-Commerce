from __future__ import annotations

import json
from dataclasses import dataclass
from http import cookiejar
from pathlib import Path
from typing import Any
from urllib import error, parse, request


BASE_URL = "http://localhost:8080"
ROOT_DIR = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT_DIR / "codex_reports" / "2026-03-20-community-activity-report.md"


@dataclass(frozen=True)
class UserSeed:
    name: str
    email: str
    password: str
    post_title: str
    post_content: str


@dataclass(frozen=True)
class CommentSeed:
    author_index: int
    target_index: int
    content: str


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
        json_body: dict[str, Any] | None = None,
        form_body: dict[str, Any] | None = None,
        expect_json: bool = True,
        allow_statuses: set[int] | None = None,
    ) -> tuple[int, Any]:
        headers: dict[str, str] = {}
        data: bytes | None = None

        if json_body is not None:
            data = json.dumps(json_body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        elif form_body is not None:
            data = parse.urlencode(form_body).encode("utf-8")
            headers["Content-Type"] = "application/x-www-form-urlencoded"

        req = request.Request(
            url=f"{self.base_url}{path}",
            data=data,
            headers=headers,
            method=method,
        )

        try:
            with self.opener.open(req) as response:
                status = response.getcode()
                raw = response.read().decode("utf-8")
        except error.HTTPError as exc:
            status = exc.code
            raw = exc.read().decode("utf-8")
            if allow_statuses and status in allow_statuses:
                return status, self._decode_body(raw, expect_json)
            raise RuntimeError(f"{method} {path} failed with {status}: {raw}") from exc

        return status, self._decode_body(raw, expect_json)

    @staticmethod
    def _decode_body(raw: str, expect_json: bool) -> Any:
        if not raw:
            return None
        if expect_json:
            return json.loads(raw)
        return raw

    def register(self, seed: UserSeed) -> int:
        status, _ = self._send(
            "POST",
            "/api/register",
            json_body={
                "name": seed.name,
                "email": seed.email,
                "password": seed.password,
            },
            allow_statuses={201, 409},
        )
        return status

    def login(self, email: str, password: str) -> dict[str, Any]:
        _, payload = self._send(
            "POST",
            "/api/login",
            json_body={"email": email, "password": password},
        )
        return payload

    def fetch_items(self) -> list[dict[str, Any]]:
        _, payload = self._send("GET", "/api/items")
        return payload

    def create_post(self, title: str, content: str) -> dict[str, Any]:
        _, payload = self._send(
            "POST",
            "/api/posts",
            json_body={"title": title, "content": content},
        )
        return payload

    def add_comment(self, post_id: int, content: str) -> dict[str, Any]:
        _, payload = self._send(
            "POST",
            f"/api/posts/{post_id}/comments",
            form_body={"reply_content": content},
        )
        return payload

    def add_to_cart(self, item_id: int, quantity: int) -> dict[str, Any]:
        _, payload = self._send(
            "POST",
            f"/api/cart/items/{item_id}",
            json_body={"id": item_id, "quantity": quantity},
        )
        return payload

    def checkout(self) -> dict[str, Any]:
        _, payload = self._send("POST", "/api/orders")
        return payload

    def fetch_my_page(self) -> dict[str, Any]:
        _, payload = self._send("GET", "/api/myPage")
        return payload


USER_SEEDS: list[UserSeed] = [
    UserSeed(
        name="윤슬",
        email="style01@shop.com",
        password="pw01",
        post_title="봄 아우터 추천 부탁드려요",
        post_content=(
            "요즘 아침저녁으로 바람이 있어서 가벼운 아우터를 하나 더 사고 싶어요. "
            "시티 레더 재킷이랑 샌드 트렌치 코트 중에서 고민 중인데, 데님이랑 같이 입기에는 "
            "어느 쪽이 더 손이 자주 갈지 궁금합니다."
        ),
    ),
    UserSeed(
        name="민호",
        email="style02@shop.com",
        password="pw02",
        post_title="미드 블루 와이드 데님 받아본 분 계신가요?",
        post_content=(
            "하의 쪽은 너무 붙는 핏보다 자연스럽게 떨어지는 걸 좋아해서 미드 블루 와이드 데님을 "
            "보고 있는데요. 허리 사이즈랑 밑단 느낌이 어느 정도인지 먼저 입어보신 분 후기 듣고 싶어요."
        ),
    ),
    UserSeed(
        name="서아",
        email="style03@shop.com",
        password="pw03",
        post_title="니트 세탁 주기 다들 어떻게 가져가세요?",
        post_content=(
            "소프트 블루 니트 색감이 예뻐서 장바구니에 넣어뒀는데 관리가 걱정돼요. "
            "한두 번 입고 바로 세탁하는 편인지, 아니면 탈취 위주로 관리하는 편인지 궁금합니다."
        ),
    ),
    UserSeed(
        name="도윤",
        email="style04@shop.com",
        password="pw04",
        post_title="출근룩 셔츠 하나 추천해 주세요",
        post_content=(
            "요즘 회사에서 너무 포멀한 셔츠 말고 조금 편하게 입을 수 있는 셔츠를 찾고 있어요. "
            "캐주얼 코튼 셔츠나 데일리 스트라이프 셔츠처럼 단정하지만 답답하지 않은 느낌 있으면 추천 부탁드려요."
        ),
    ),
    UserSeed(
        name="하린",
        email="style05@shop.com",
        password="pw05",
        post_title="비 오는 날 아우터는 뭐가 제일 실용적일까요",
        post_content=(
            "요즘처럼 날씨 애매할 때는 드레이프 롱 코트보다 짧은 재킷이 더 실용적인지 고민돼요. "
            "비 오는 날에도 무겁지 않고 관리 편한 아우터 있으시면 공유 부탁드릴게요."
        ),
    ),
    UserSeed(
        name="태오",
        email="style06@shop.com",
        password="pw06",
        post_title="후디 하나만 산다면 어떤 컬러가 제일 활용도 높을까요?",
        post_content=(
            "미니멀 테리 후디 느낌이 좋아 보이는데, 이런 기본 후디는 결국 자주 입는 색이 제일 중요하더라고요. "
            "검정 아우터 안에 받쳐 입기 좋은 컬러 조합 있으시면 추천 부탁드립니다."
        ),
    ),
    UserSeed(
        name="수빈",
        email="style07@shop.com",
        password="pw07",
        post_title="데님이랑 치노 중에 이번 주말 뭐가 더 나을까요",
        post_content=(
            "주말에 오래 걷는 일정이 있어서 클래식 스트레이트 진이 나을지, 베이지 치노 팬츠가 나을지 고민입니다. "
            "편한 쪽으로 고르고 싶은데 실제 착용감 차이가 있는지 궁금해요."
        ),
    ),
    UserSeed(
        name="지안",
        email="style08@shop.com",
        password="pw08",
        post_title="첫 주문 배송 만족해서 후기 남겨요",
        post_content=(
            "처음 주문해봤는데 생각보다 배송이 빨랐고 포장도 깔끔해서 인상 좋았습니다. "
            "에센셜 오버핏 반팔이랑 커브드 핏 데님 조합으로 입어봤는데 데일리하게 딱 좋네요."
        ),
    ),
    UserSeed(
        name="현우",
        email="style09@shop.com",
        password="pw09",
        post_title="화이트 스니커즈에 어울리는 팬츠 추천 부탁드려요",
        post_content=(
            "신발은 밝게 가고 상의는 무난하게 입는 편이라서 팬츠 핏이 전체 느낌을 많이 좌우하더라고요. "
            "라이트 워시 데님 팬츠나 이지 코튼 트라우저 중에 더 깔끔한 쪽이 뭘지 의견 부탁드립니다."
        ),
    ),
    UserSeed(
        name="나연",
        email="style10@shop.com",
        password="pw10",
        post_title="이번 주말 쇼핑 리스트 공유해 봐요",
        post_content=(
            "저는 이번 주말에 포멀 네이비 블레이저, 크림 베이직 스웨트셔츠, 빈티지 워시 진 순서로 보고 있어요. "
            "다들 요즘 장바구니에 담아둔 제품 있으면 같이 공유해 주세요."
        ),
    ),
]


COMMENT_SEEDS: list[CommentSeed] = [
    CommentSeed(0, 1, "와이드 데님은 허벅지부터 떨어지는 라인이 중요하던데 질문 포인트가 딱 좋네요."),
    CommentSeed(0, 9, "쇼핑 리스트 구성이 균형감 있어서 저도 그대로 저장해두고 싶어요."),
    CommentSeed(1, 2, "니트는 자주 세탁하기보다 하루 쉬게 두는 편이 오래 입기 좋더라고요."),
    CommentSeed(1, 7, "첫 주문 만족 후기 보니까 저도 다음 장바구니는 바로 결제하게 될 것 같아요."),
    CommentSeed(2, 3, "출근룩은 스트라이프 셔츠 하나만 있어도 분위기가 꽤 달라져서 공감했어요."),
    CommentSeed(2, 8, "화이트 스니커즈에는 밑단이 너무 무겁지 않은 팬츠가 잘 맞더라고요."),
    CommentSeed(3, 0, "레더 재킷은 간단한 티셔츠 위에만 걸쳐도 분위기 살아서 손이 자주 갈 것 같아요."),
    CommentSeed(3, 4, "비 오는 날에는 길이감보다 관리 편한 소재가 결국 제일 중요하더라고요."),
    CommentSeed(4, 5, "후디는 결국 아우터 안에 겹쳐 입기 쉬운 톤이 제일 오래 입게 되는 것 같아요."),
    CommentSeed(4, 6, "주말용이면 치노보다 데님이 더 편할 때도 많아서 고민되는 포인트 공감합니다."),
    CommentSeed(5, 3, "캐주얼 코튼 셔츠 쪽이 출근룩으로 더 자주 손이 갈 것 같아요."),
    CommentSeed(5, 9, "블레이저랑 워시 진 같이 보는 조합 좋네요. 주말에도 무리 없을 것 같아요."),
    CommentSeed(6, 1, "허리보다 밑위 길이가 편한지도 같이 보면 실패 확률이 줄더라고요."),
    CommentSeed(6, 8, "화이트 스니커즈면 너무 와이드한 팬츠보다 정돈된 핏이 예쁘게 보였어요."),
    CommentSeed(7, 0, "트렌치 코트 쪽이 봄에는 활용도 높을 것 같아서 저도 궁금했던 주제예요."),
    CommentSeed(7, 5, "후디는 한번 사면 오래 입으니까 기본 색 먼저 가는 게 맞는 것 같아요."),
    CommentSeed(8, 4, "비 오는 날엔 짧은 재킷이 확실히 관리가 편해서 저도 그쪽으로 기울어요."),
    CommentSeed(8, 7, "배송 만족 후기 남겨주셔서 반갑네요. 이런 글 보면 쇼핑할 때 신뢰가 올라가요."),
    CommentSeed(9, 2, "니트는 소재만 잘 관리하면 시즌마다 꺼내 입기 좋아서 저도 관리법 궁금했어요."),
    CommentSeed(9, 6, "오래 걷는 날이면 생각보다 허벅지 여유 있는 팬츠가 더 편하더라고요."),
]


def ensure_dir(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def group_items_by_category(items: list[dict[str, Any]]) -> dict[int, list[dict[str, Any]]]:
    grouped: dict[int, list[dict[str, Any]]] = {}
    for item in sorted(items, key=lambda entry: entry["id"]):
        category_id = item.get("categoryId")
        if category_id is None:
            continue
        grouped.setdefault(category_id, []).append(item)
    return grouped


def build_report(
    summaries: list[dict[str, Any]],
    comments_created: list[dict[str, Any]],
) -> str:
    lines: list[str] = [
        "# 2026-03-20 커뮤니티/회원 활동 시드 보고서",
        "",
        "## 1. 작업 요약",
        "",
        "- 실제 API를 통해 회원 10명을 생성했습니다.",
        "- 각 회원은 게시글 1개씩 작성했고, 서로 댓글을 남기도록 커뮤니티 데이터를 채웠습니다.",
        "- 각 회원은 실제 상품을 장바구니에 담고 주문 1회를 완료한 뒤, 현재 장바구니에도 상품 1개가 남아 있도록 상태를 만들었습니다.",
        "",
        "## 2. 계정 정보",
        "",
        "| 이름 | 이메일 | 비밀번호 |",
        "| --- | --- | --- |",
    ]

    for summary in summaries:
        lines.append(
            f"| {summary['name']} | {summary['email']} | {summary['password']} |"
        )

    lines.extend(
        [
            "",
            "## 3. 회원별 생성 결과",
            "",
            "| 이름 | 게시글 ID | 게시글 제목 | 주문 ID | 주문 상품 | 현재 장바구니 | 댓글 작성 수 |",
            "| --- | --- | --- | --- | --- | --- | --- |",
        ]
    )

    for summary in summaries:
        ordered_names = ", ".join(summary["ordered_item_names"])
        cart_names = ", ".join(summary["cart_item_names"])
        lines.append(
            f"| {summary['name']} | {summary['post_id']} | {summary['post_title']} | "
            f"{summary['order_id']} | {ordered_names} | {cart_names} | {summary['comment_count']} |"
        )

    lines.extend(
        [
            "",
            "## 4. 생성된 게시글",
            "",
        ]
    )

    for summary in summaries:
        lines.extend(
            [
                f"### {summary['name']} - {summary['post_title']}",
                "",
                f"- 게시글 ID: `{summary['post_id']}`",
                f"- 내용: {summary['post_content']}",
                "",
            ]
        )

    lines.extend(
        [
            "## 5. 댓글 활동 요약",
            "",
            f"- 총 생성 댓글 수: `{len(comments_created)}`",
            "",
            "| 작성자 | 대상 게시글 ID | 댓글 ID | 내용 |",
            "| --- | --- | --- | --- |",
        ]
    )

    for comment in comments_created:
        lines.append(
            f"| {comment['author_name']} | {comment['post_id']} | {comment['comment_id']} | {comment['content']} |"
        )

    lines.extend(
        [
            "",
            "## 6. 최종 상태 요약",
            "",
            "- 회원 수: 10명",
            "- 신규 게시글: 10개",
            "- 신규 댓글: 20개",
            "- 신규 주문: 10건",
            "- 현재 장바구니를 가진 회원: 10명",
            "",
            "## 7. 비고",
            "",
            "- 이번 작업은 모두 실제 백엔드 API를 통해 반영되었습니다.",
            "- 회원, 게시글, 댓글, 장바구니, 주문 데이터는 실제 DB에 저장되었습니다.",
            "- 주문에 사용한 상품 이미지는 기존에 연결된 S3 이미지를 그대로 사용합니다.",
        ]
    )

    return "\n".join(lines) + "\n"


def main() -> None:
    public_client = ApiClient(BASE_URL)
    items = public_client.fetch_items()
    grouped_items = group_items_by_category(items)

    tops = grouped_items.get(6, [])
    bottoms = grouped_items.get(7, [])
    outers = grouped_items.get(8, [])

    if len(tops) < 10 or len(bottoms) < 10 or len(outers) < 10:
        raise RuntimeError("카테고리별 상품 수가 부족합니다. 상의/하의/아우터가 각각 10개 이상 필요합니다.")

    post_ids: list[int] = []
    user_clients: list[ApiClient] = []
    summaries: list[dict[str, Any]] = []

    for index, seed in enumerate(USER_SEEDS):
        client = ApiClient(BASE_URL)
        register_status = client.register(seed)
        client.login(seed.email, seed.password)

        post = client.create_post(seed.post_title, seed.post_content)
        post_id = int(post["id"])
        post_ids.append(post_id)

        top_item = tops[index]
        bottom_item = bottoms[index]
        outer_item = outers[index]

        client.add_to_cart(int(top_item["id"]), 1)
        client.add_to_cart(int(bottom_item["id"]), 1)
        order = client.checkout()
        client.add_to_cart(int(outer_item["id"]), 1)
        my_page = client.fetch_my_page()

        summaries.append(
            {
                "name": seed.name,
                "email": seed.email,
                "password": seed.password,
                "register_status": register_status,
                "post_id": post_id,
                "post_title": seed.post_title,
                "post_content": seed.post_content,
                "order_id": int(order["id"]),
                "ordered_item_names": [line["itemName"] for line in order["orderItems"]],
                "cart_item_names": [item["itemName"] for item in my_page.get("cartItems", [])],
                "comment_count": 0,
            }
        )
        user_clients.append(client)

    comments_created: list[dict[str, Any]] = []
    for comment_seed in COMMENT_SEEDS:
        author_summary = summaries[comment_seed.author_index]
        target_post_id = post_ids[comment_seed.target_index]
        client = user_clients[comment_seed.author_index]
        comment = client.add_comment(target_post_id, comment_seed.content)
        author_summary["comment_count"] += 1
        comments_created.append(
            {
                "author_name": author_summary["name"],
                "post_id": target_post_id,
                "comment_id": int(comment["id"]),
                "content": comment_seed.content,
            }
        )

    report = build_report(summaries, comments_created)
    ensure_dir(REPORT_PATH)
    REPORT_PATH.write_text(report, encoding="utf-8")

    print(f"Created {len(USER_SEEDS)} users, {len(USER_SEEDS)} posts, {len(comments_created)} comments.")
    print(f"Report written to: {REPORT_PATH}")


if __name__ == "__main__":
    main()
