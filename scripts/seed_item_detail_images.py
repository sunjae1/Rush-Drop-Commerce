from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib import parse

try:
    import pymysql
except ImportError as exc:  # pragma: no cover - runtime guidance
    raise SystemExit(
        "PyMySQL is required. Install it with: pip install -r scripts/requirements-perf.txt"
    ) from exc


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ENV_FILE = PROJECT_ROOT / "shopping_be" / ".env"


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    user: str
    password: str
    database: str


@dataclass(frozen=True)
class ItemRow:
    id: int
    name: str
    category_name: str | None


DETAIL_IMAGE_POOLS: dict[str, list[str]] = {
    "상의": [
        "https://images.pexels.com/photos/9775681/pexels-photo-9775681.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/35408208/pexels-photo-35408208.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/14463985/pexels-photo-14463985.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/34262675/pexels-photo-34262675.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "하의": [
        "https://images.pexels.com/photos/4109798/pexels-photo-4109798.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/33706493/pexels-photo-33706493.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/29191569/pexels-photo-29191569.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/35568704/pexels-photo-35568704.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "아우터": [
        "https://images.pexels.com/photos/12148300/pexels-photo-12148300.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/9968540/pexels-photo-9968540.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/8343180/pexels-photo-8343180.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/19487834/pexels-photo-19487834.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "신발": [
        "https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/15398044/pexels-photo-15398044.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/29548615/pexels-photo-29548615.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/9333372/pexels-photo-9333372.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "가방": [
        "https://images.pexels.com/photos/29096395/pexels-photo-29096395.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/34997535/pexels-photo-34997535.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/34027219/pexels-photo-34027219.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/36364966/pexels-photo-36364966.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "모자": [
        "https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/13697753/pexels-photo-13697753.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/31162881/pexels-photo-31162881.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/18313293/pexels-photo-18313293.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "시계": [
        "https://images.pexels.com/photos/9395501/pexels-photo-9395501.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/11780439/pexels-photo-11780439.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/34031837/pexels-photo-34031837.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/3083461/pexels-photo-3083461.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "머플러": [
        "https://images.pexels.com/photos/34108352/pexels-photo-34108352.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/36211083/pexels-photo-36211083.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/30327445/pexels-photo-30327445.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/32721785/pexels-photo-32721785.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "액세서리": [
        "https://images.pexels.com/photos/29986281/pexels-photo-29986281.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/36229132/pexels-photo-36229132.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/10596294/pexels-photo-10596294.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/32989031/pexels-photo-32989031.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "벨트": [
        "https://images.pexels.com/photos/32498751/pexels-photo-32498751.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/35392702/pexels-photo-35392702.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/35392722/pexels-photo-35392722.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/32498611/pexels-photo-32498611.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "선글라스": [
        "https://images.pexels.com/photos/32677205/pexels-photo-32677205.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/701877/pexels-photo-701877.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/15848006/pexels-photo-15848006.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/9957406/pexels-photo-9957406.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "키즈": [
        "https://images.pexels.com/photos/28808342/pexels-photo-28808342.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/34608858/pexels-photo-34608858.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/35338045/pexels-photo-35338045.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/14622860/pexels-photo-14622860.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
    "DEFAULT": [
        "https://images.pexels.com/photos/9775681/pexels-photo-9775681.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/29096395/pexels-photo-29096395.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg?auto=compress&cs=tinysrgb&w=1400",
        "https://images.pexels.com/photos/29986281/pexels-photo-29986281.jpeg?auto=compress&cs=tinysrgb&w=1400",
    ],
}

DETAIL_SLOTS = (
    ("MOOD", "착용 컷", "{item_name} 착용 무드 이미지"),
    ("MOOD", "무드 컷", "{item_name} 스타일링 무드 이미지"),
    ("DETAIL", "소재 디테일", "{item_name} 소재와 디테일 이미지"),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "현재 개발 DB의 item 테이블을 기준으로 상품별 상세 이미지를 "
            "item_detail_image 테이블에 시드합니다."
        )
    )
    parser.add_argument("--env-file", type=Path, default=DEFAULT_ENV_FILE)
    parser.add_argument("--host")
    parser.add_argument("--port", type=int)
    parser.add_argument("--user")
    parser.add_argument("--password")
    parser.add_argument("--database")
    parser.add_argument("--item-id", type=int, help="특정 상품만 시드합니다.")
    parser.add_argument("--limit", type=int, help="처리할 상품 수를 제한합니다.")
    parser.add_argument(
        "--replace",
        action="store_true",
        help="대상 상품의 기존 상세 이미지를 삭제하고 다시 생성합니다.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="DB에 쓰지 않고 생성될 행 수만 확인합니다.",
    )
    return parser.parse_args()


def load_env_file(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}

    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def parse_jdbc_mysql_url(jdbc_url: str) -> tuple[str, int, str]:
    if not jdbc_url.startswith("jdbc:mysql://"):
        raise RuntimeError(f"지원하지 않는 DB_URL 형식입니다: {jdbc_url}")

    parsed = parse.urlparse("mysql://" + jdbc_url[len("jdbc:mysql://"):])
    if not parsed.hostname:
        raise RuntimeError(f"DB_URL에서 host를 읽을 수 없습니다: {jdbc_url}")

    database = parsed.path.lstrip("/")
    if not database:
        raise RuntimeError(f"DB_URL에서 database를 읽을 수 없습니다: {jdbc_url}")

    return parsed.hostname, parsed.port or 3306, database


def build_db_config(args: argparse.Namespace) -> DbConfig:
    env = load_env_file(args.env_file)
    host = "127.0.0.1"
    port = 3306
    database = "myshopping"

    db_url = env.get("DB_URL")
    if db_url:
        host, port, database = parse_jdbc_mysql_url(db_url)

    return DbConfig(
        host=args.host or host,
        port=args.port or port,
        user=args.user or env.get("DB_USERNAME", "root"),
        password=args.password if args.password is not None else env.get("DB_PASSWORD", ""),
        database=args.database or database,
    )


def fetch_items(cursor: Any, item_id: int | None, limit: int | None) -> list[ItemRow]:
    params: list[Any] = []
    where = "where i.deleted = false"
    if item_id is not None:
        where += " and i.id = %s"
        params.append(item_id)

    sql = f"""
        select i.id, i.item_name, c.name
        from item i
        left join category c on c.id = i.category_id
        {where}
        order by i.id
    """
    if limit is not None:
        sql += " limit %s"
        params.append(limit)

    cursor.execute(sql, params)
    return [ItemRow(id=row[0], name=row[1], category_name=row[2]) for row in cursor.fetchall()]


def fetch_existing_item_ids(cursor: Any, item_ids: list[int]) -> set[int]:
    if not item_ids:
        return set()

    placeholders = ", ".join(["%s"] * len(item_ids))
    cursor.execute(
        f"""
        select distinct item_id
        from item_detail_image
        where item_id in ({placeholders})
        """,
        item_ids,
    )
    return {int(row[0]) for row in cursor.fetchall()}


def build_detail_rows(items: list[ItemRow]) -> list[tuple[Any, ...]]:
    rows: list[tuple[Any, ...]] = []
    for item in items:
        pool = DETAIL_IMAGE_POOLS.get(item.category_name or "", DETAIL_IMAGE_POOLS["DEFAULT"])
        offset = item.id % len(pool)

        for index, (image_role, caption, alt_template) in enumerate(DETAIL_SLOTS, start=1):
            image_url = pool[(offset + index - 1) % len(pool)]
            rows.append(
                (
                    item.id,
                    index,
                    image_role,
                    image_url,
                    alt_template.format(item_name=item.name),
                    caption,
                )
            )

    return rows


def seed_detail_images(connection: Any, args: argparse.Namespace) -> tuple[int, int, int]:
    with connection.cursor() as cursor:
        items = fetch_items(cursor, args.item_id, args.limit)
        if not items:
            return 0, 0, 0

        target_items = items
        if args.replace:
            item_ids = [item.id for item in target_items]
            placeholders = ", ".join(["%s"] * len(item_ids))
            cursor.execute(
                f"delete from item_detail_image where item_id in ({placeholders})",
                item_ids,
            )
        else:
            existing_item_ids = fetch_existing_item_ids(cursor, [item.id for item in items])
            target_items = [item for item in items if item.id not in existing_item_ids]

        rows = build_detail_rows(target_items)
        if args.dry_run:
            return len(items), len(target_items), len(rows)

        if rows:
            cursor.executemany(
                """
                insert into item_detail_image
                  (item_id, display_order, image_role, image_url, alt_text, caption)
                values (%s, %s, %s, %s, %s, %s)
                on duplicate key update
                  image_role = values(image_role),
                  image_url = values(image_url),
                  alt_text = values(alt_text),
                  caption = values(caption)
                """,
                rows,
            )

    connection.commit()
    return len(items), len(target_items), len(rows)


def main() -> None:
    args = parse_args()
    config = build_db_config(args)

    connection = pymysql.connect(
        host=config.host,
        port=config.port,
        user=config.user,
        password=config.password,
        database=config.database,
        charset="utf8mb4",
        autocommit=False,
    )
    try:
        scanned, seeded_items, seeded_rows = seed_detail_images(connection, args)
    finally:
        connection.close()

    mode = "DRY RUN" if args.dry_run else "SEEDED"
    print(f"[{mode}] scanned_items={scanned} target_items={seeded_items} detail_image_rows={seeded_rows}")


if __name__ == "__main__":
    main()
