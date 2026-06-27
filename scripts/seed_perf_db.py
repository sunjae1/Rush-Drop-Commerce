from __future__ import annotations

import argparse
import math
import random
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta

try:
    import pymysql
except ImportError as exc:  # pragma: no cover - runtime guidance
    raise SystemExit(
        "PyMySQL is required. Install it with: pip install -r scripts/requirements-perf.txt"
    ) from exc


DEFAULT_IMAGE_KEYS = [
    "images/perf-shared-1.webp",
    "images/perf-shared-2.webp",
    "images/perf-shared-3.webp",
    "images/perf-shared-4.webp",
]
DEFAULT_PASSWORD = "{noop}pw1234!"
ORDER_STATUSES = ("ORDERED", "PAID", "CANCELLED")


@dataclass(frozen=True)
class MemberRow:
    id: int
    name: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Seed a local MySQL database for DB/index performance testing."
    )
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3307)
    parser.add_argument("--user", default="shopping")
    parser.add_argument("--password", default="shopping123!")
    parser.add_argument("--database", default="myshopping_perf")
    parser.add_argument("--run-tag", default=datetime.now().strftime("%Y%m%d%H%M%S"))
    parser.add_argument("--batch-size", type=int, default=1000)
    parser.add_argument("--category-count", type=int, default=20)
    parser.add_argument("--member-count", type=int, default=1000)
    parser.add_argument("--item-count", type=int, default=5000)
    parser.add_argument("--post-count", type=int, default=10000)
    parser.add_argument("--comment-count", type=int, default=30000)
    parser.add_argument("--order-count", type=int, default=10000)
    parser.add_argument("--order-item-count", type=int, default=30000)
    parser.add_argument("--cart-item-count", type=int, default=5000)
    return parser.parse_args()


def chunked(values: list[tuple], batch_size: int):
    for index in range(0, len(values), batch_size):
        yield values[index : index + batch_size]


def build_content(prefix: str, run_tag: str, index: int, target_length: int = 120) -> str:
    base = f"{prefix} {run_tag} #{index} "
    repeats = max(1, math.ceil(target_length / len(base)))
    return (base * repeats)[:target_length]


def random_timestamp(seed: int, days: int = 365) -> datetime:
    random_generator = random.Random(seed)
    start = datetime.now() - timedelta(days=days)
    return start + timedelta(seconds=random_generator.randint(0, days * 24 * 60 * 60))


def load_existing_image_keys(cursor) -> list[str]:
    cursor.execute(
        """
        select distinct image_url
        from item
        where image_url is not null
          and image_url <> ''
        limit 8
        """
    )
    keys = [row[0] for row in cursor.fetchall() if row[0]]
    return keys or DEFAULT_IMAGE_KEYS


def insert_categories(cursor, run_tag: str, category_count: int, batch_size: int) -> list[int]:
    rows = [(f"PERF_CATEGORY_{run_tag}_{index + 1}",) for index in range(category_count)]
    for batch in chunked(rows, batch_size):
        cursor.executemany("insert into category (name) values (%s)", batch)

    cursor.execute(
        """
        select id
        from category
        where name like %s
        order by id
        """,
        (f"PERF_CATEGORY_{run_tag}_%",),
    )
    return [row[0] for row in cursor.fetchall()]


def insert_members(cursor, run_tag: str, member_count: int, batch_size: int) -> list[MemberRow]:
    rows = []
    for index in range(member_count):
        rows.append(
            (
                f"perf_{run_tag}_{index + 1}@example.com",
                f"Perf User {index + 1}",
                DEFAULT_PASSWORD,
                True,
                "USER",
            )
        )

    for batch in chunked(rows, batch_size):
        cursor.executemany(
            """
            insert into member (email, name, password, active, role)
            values (%s, %s, %s, %s, %s)
            """,
            batch,
        )

    cursor.execute(
        """
        select id, name
        from member
        where email like %s
        order by id
        """,
        (f"perf_{run_tag}_%@example.com",),
    )
    return [MemberRow(id=row[0], name=row[1]) for row in cursor.fetchall()]


def insert_items(
    cursor,
    run_tag: str,
    item_count: int,
    batch_size: int,
    category_ids: list[int],
    image_keys: list[str],
) -> list[int]:
    rows = []
    for index in range(item_count):
        rows.append(
            (
                False,
                1000 + ((index % 200) * 250),
                20 + (index % 80),
                category_ids[index % len(category_ids)],
                image_keys[index % len(image_keys)],
                f"PERF_ITEM_{run_tag}_{index + 1}",
            )
        )

    for batch in chunked(rows, batch_size):
        cursor.executemany(
            """
            insert into item (deleted, price, quantity, category_id, image_url, item_name)
            values (%s, %s, %s, %s, %s, %s)
            """,
            batch,
        )

    cursor.execute(
        """
        select id
        from item
        where item_name like %s
        order by id
        """,
        (f"PERF_ITEM_{run_tag}_%",),
    )
    return [row[0] for row in cursor.fetchall()]


def insert_posts(
    cursor,
    run_tag: str,
    post_count: int,
    batch_size: int,
    members: list[MemberRow],
) -> list[int]:
    rows = []
    for index in range(post_count):
        member = members[index % len(members)]
        rows.append(
            (
                random_timestamp(index + 1000),
                member.id,
                member.name,
                build_content("PERF_POST_BODY", run_tag, index + 1, 180),
                f"PERF_POST_{run_tag}_{index + 1}",
            )
        )

    for batch in chunked(rows, batch_size):
        cursor.executemany(
            """
            insert into post (created_date, user_id, author, content, title)
            values (%s, %s, %s, %s, %s)
            """,
            batch,
        )

    cursor.execute(
        """
        select id
        from post
        where title like %s
        order by id
        """,
        (f"PERF_POST_{run_tag}_%",),
    )
    return [row[0] for row in cursor.fetchall()]


def insert_comments(
    cursor,
    run_tag: str,
    comment_count: int,
    batch_size: int,
    member_ids: list[int],
    post_ids: list[int],
) -> None:
    rows = []
    for index in range(comment_count):
        rows.append(
            (
                random_timestamp(index + 50000),
                post_ids[index % len(post_ids)],
                member_ids[(index * 7) % len(member_ids)],
                build_content("PERF_COMMENT", run_tag, index + 1, 120)[:255],
            )
        )

    for batch in chunked(rows, batch_size):
        cursor.executemany(
            """
            insert into comment (created_date, post_id, user_id, content)
            values (%s, %s, %s, %s)
            """,
            batch,
        )


def insert_orders(
    cursor,
    order_count: int,
    batch_size: int,
    member_ids: list[int],
) -> list[int]:
    base_time = datetime.now() - timedelta(days=180)
    rows = []
    for index in range(order_count):
        rows.append(
            (
                base_time + timedelta(seconds=index),
                member_ids[index % len(member_ids)],
                ORDER_STATUSES[index % len(ORDER_STATUSES)],
            )
        )

    for batch in chunked(rows, batch_size):
        cursor.executemany(
            """
            insert into orders (order_date, user_id, status)
            values (%s, %s, %s)
            """,
            batch,
        )

    cursor.execute(
        """
        select id
        from orders
        order by id desc
        limit %s
        """,
        (order_count,),
    )
    rows = [row[0] for row in cursor.fetchall()]
    rows.reverse()
    return rows


def insert_order_items(
    cursor,
    order_item_count: int,
    batch_size: int,
    order_ids: list[int],
    item_ids: list[int],
) -> None:
    rows = []
    for index in range(order_item_count):
        item_id = item_ids[index % len(item_ids)]
        quantity = (index % 5) + 1
        order_price = 1000 + ((index % 200) * 250)
        rows.append((order_price, quantity, item_id, order_ids[index % len(order_ids)]))

    for batch in chunked(rows, batch_size):
        cursor.executemany(
            """
            insert into order_item (order_price, quantity, item_id, order_id)
            values (%s, %s, %s, %s)
            """,
            batch,
        )


def insert_carts(cursor, member_ids: list[int], batch_size: int) -> list[int]:
    rows = [(member_id,) for member_id in member_ids]
    for batch in chunked(rows, batch_size):
        cursor.executemany("insert into cart (user_id) values (%s)", batch)

    cursor.execute(
        """
        select id
        from cart
        order by id desc
        limit %s
        """,
        (len(member_ids),),
    )
    rows = [row[0] for row in cursor.fetchall()]
    rows.reverse()
    return rows


def insert_cart_items(
    cursor,
    cart_item_count: int,
    batch_size: int,
    cart_ids: list[int],
    item_ids: list[int],
) -> None:
    rows = []
    for index in range(cart_item_count):
        rows.append(
            (
                (index % 3) + 1,
                cart_ids[index % len(cart_ids)],
                item_ids[(index * 5) % len(item_ids)],
            )
        )

    for batch in chunked(rows, batch_size):
        cursor.executemany(
            """
            insert into cart_item (quantity, cart_id, item_id)
            values (%s, %s, %s)
            """,
            batch,
        )


def main() -> None:
    args = parse_args()
    category_ids: list[int] = []
    item_ids: list[int] = []
    post_ids: list[int] = []
    connection = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
        autocommit=False,
    )

    try:
        with connection.cursor() as cursor:
            image_keys = load_existing_image_keys(cursor)
            category_ids = insert_categories(cursor, args.run_tag, args.category_count, args.batch_size)
            members = insert_members(cursor, args.run_tag, args.member_count, args.batch_size)
            member_ids = [member.id for member in members]
            item_ids = insert_items(
                cursor,
                args.run_tag,
                args.item_count,
                args.batch_size,
                category_ids,
                image_keys,
            )
            post_ids = insert_posts(cursor, args.run_tag, args.post_count, args.batch_size, members)
            insert_comments(
                cursor,
                args.run_tag,
                args.comment_count,
                args.batch_size,
                member_ids,
                post_ids,
            )
            order_ids = insert_orders(cursor, args.order_count, args.batch_size, member_ids)
            insert_order_items(
                cursor,
                args.order_item_count,
                args.batch_size,
                order_ids,
                item_ids,
            )
            cart_ids = insert_carts(cursor, member_ids, args.batch_size)
            insert_cart_items(
                cursor,
                args.cart_item_count,
                args.batch_size,
                cart_ids,
                item_ids,
            )

        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    print("Seed completed.")
    print(f"run_tag={args.run_tag}")
    print(f"categories={args.category_count}")
    print(f"members={args.member_count}")
    print(f"items={args.item_count}")
    print(f"posts={args.post_count}")
    print(f"comments={args.comment_count}")
    print(f"orders={args.order_count}")
    print(f"order_items={args.order_item_count}")
    print(f"cart_items={args.cart_item_count}")
    if category_ids:
        print(f"sample_category_id={category_ids[0]}")
    if item_ids:
        print(f"sample_item_id={item_ids[0]}")
    if post_ids:
        print(f"sample_post_id={post_ids[0]}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(130)
