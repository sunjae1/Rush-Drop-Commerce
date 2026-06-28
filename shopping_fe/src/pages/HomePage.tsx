import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  fetchCategories,
  fetchItems,
  fetchPosts,
  toAppErrorMessage
} from "../api/client";
import type { Category, Item, Post } from "../api/types";
import { ProductCard } from "../components/ProductCard";
import { StatusBanner } from "../components/StatusBanner";
import {
  getDropSaleLabel,
  getDropSaleScheduleText,
  getDropSaleStatus
} from "../lib/drop";
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  formatNumber,
  getItemAvailability,
  resolveImageUrl
} from "../lib/format";

function getDailySeedValue(date: Date): number {
  const dateKey = [
    String(date.getFullYear()),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0")
  ].join("");

  return dateKey.split("").reduce((total, digit, index) => {
    return total + Number(digit) * (index + 1);
  }, 0);
}

function getDailyHeroItem(items: Item[]): Item | null {
  if (items.length === 0) {
    return null;
  }

  const dropCandidates = items.filter((entry) => {
    const status = getDropSaleStatus(entry);

    return entry.dropProduct && entry.quantity > 0 && (status === "LIVE" || status === "UPCOMING");
  });
  const candidateItems = dropCandidates.length > 0
    ? dropCandidates
    : items.filter((entry) => entry.quantity > 0);
  const sourceItems = candidateItems.length > 0 ? candidateItems : items;
  const heroIndex = getDailySeedValue(new Date()) % sourceItems.length;

  return sourceItems[heroIndex] ?? sourceItems[0] ?? null;
}

export function HomePage() {
  const [items, setItems] = useState<Item[]>([]);
  const [visibleItems, setVisibleItems] = useState<Item[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const catalogResultsRef = useRef<HTMLDivElement | null>(null);
  const pendingSearchTopRef = useRef<number | null>(null);
  const [catalogResultsMinHeight, setCatalogResultsMinHeight] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const [nextItems, nextCategories, nextPosts] = await Promise.all([
          fetchItems(),
          fetchCategories().catch(() => []),
          fetchPosts().catch(() => [])
        ]);

        if (cancelled) {
          return;
        }

        setItems(nextItems);
        setVisibleItems(nextItems);
        setCategories(nextCategories);
        setPosts(nextPosts);
      } catch (nextError) {
        if (!cancelled) {
          setError(toAppErrorMessage(nextError, "메인 화면을 불러오지 못했습니다."));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    if (loading) {
      return () => undefined;
    }

    if (!query.trim() && selectedCategoryId === null) {
      setVisibleItems(items);
      setCatalogLoading(false);
      return () => undefined;
    }

    async function loadFilteredItems() {
      setCatalogLoading(true);
      setError(null);

      try {
        const nextItems = await fetchItems({
          keyword: query,
          categoryId: selectedCategoryId
        });

        if (cancelled) {
          return;
        }

        setVisibleItems(nextItems);
      } catch (nextError) {
        if (!cancelled) {
          setError(toAppErrorMessage(nextError, "상품 필터 결과를 불러오지 못했습니다."));
        }
      } finally {
        if (!cancelled) {
          setCatalogLoading(false);
        }
      }
    }

    void loadFilteredItems();

    return () => {
      cancelled = true;
    };
  }, [items, loading, query, selectedCategoryId]);

  const heroItem = getDailyHeroItem(items);
  const shortcutCollections = categories;
  const availableItemsCount = items.filter((item) => item.quantity > 0).length;
  const dropItems = items.filter((item) => item.dropProduct);
  const liveDropItems = dropItems.filter((item) => getDropSaleStatus(item) === "LIVE");
  const upcomingDropItems = dropItems.filter((item) => getDropSaleStatus(item) === "UPCOMING");
  const endedDropItems = dropItems.filter((item) => getDropSaleStatus(item) === "ENDED");
  const dropHeroLabel = heroItem ? getDropSaleLabel(heroItem) : null;
  const dropHeroSchedule = heroItem ? getDropSaleScheduleText(heroItem) : null;
  const premiumItems = [...items].sort((left, right) => right.price - left.price).slice(0, 3);
  const lowStockItems = [...items]
    .filter((item) => item.quantity > 0)
    .sort((left, right) => left.quantity - right.quantity)
    .slice(0, 3);
  const communityHighlights = posts.slice(0, 3);
  const promoCards = [
    {
      eyebrow: "LIVE DROPS",
      title: `${formatNumber(liveDropItems.length)}개`,
      description: "지금 선착순으로 열려 있는 드롭"
    },
    {
      eyebrow: "NEXT DROPS",
      title: `${formatNumber(upcomingDropItems.length)}개`,
      description: "오픈 시간을 기다리는 한정 수량 상품"
    },
    communityHighlights[0]
      ? {
          eyebrow: "DROP TALK",
          title: communityHighlights[0].title,
          description: communityHighlights[0].author,
          meta: formatDate(communityHighlights[0].createdDate)
        }
      : {
          eyebrow: "DROP TALK",
          title: "첫 구매 후기를 기다리는 중",
          description: "오픈 대기, 성공 후기, 사이즈 팁을 나눠보세요."
        }
  ];
  const selectedCategory = categories.find((category) => category.id === selectedCategoryId) ?? null;

  useLayoutEffect(() => {
    if (pendingSearchTopRef.current === null) {
      return;
    }

    const searchInput = searchInputRef.current;

    if (!searchInput) {
      pendingSearchTopRef.current = null;
      return;
    }

    const nextTop = searchInput.getBoundingClientRect().top;
    const delta = nextTop - pendingSearchTopRef.current;

    if (delta !== 0) {
      window.scrollBy(0, delta);
    }

    pendingSearchTopRef.current = null;
  }, [catalogLoading, loading, query, selectedCategoryId, visibleItems.length]);

  function preserveCatalogViewport() {
    const searchInput = searchInputRef.current;
    const catalogResults = catalogResultsRef.current;

    if (searchInput) {
      pendingSearchTopRef.current = searchInput.getBoundingClientRect().top;
    }

    if (catalogResults) {
      const currentHeight = catalogResults.getBoundingClientRect().height;
      setCatalogResultsMinHeight((previousHeight) => Math.max(previousHeight ?? 0, currentHeight));
    }
  }

  function handleQueryChange(nextQuery: string) {
    if (!nextQuery.trim() && selectedCategoryId === null) {
      setCatalogResultsMinHeight(null);
    } else {
      preserveCatalogViewport();
    }

    setQuery(nextQuery);
  }

  function handleCategoryChange(nextCategoryId: number | null) {
    if (nextCategoryId === null && !query.trim()) {
      setCatalogResultsMinHeight(null);
    } else {
      preserveCatalogViewport();
    }

    setSelectedCategoryId(nextCategoryId);
  }

  function handleShortcutClick(categoryId: number) {
    handleCategoryChange(selectedCategoryId === categoryId ? null : categoryId);
    document.getElementById("catalog")?.scrollIntoView({
      behavior: "smooth",
      block: "start"
    });
  }

  return (
    <div className="page-stack home-page">
      <section className="home-hero">
        <div className="hero-copy">
          <div className="hero-copy-main">
            <p className="eyebrow">LIMITED DROP COMMERCE</p>
            <h1>선착순 한정 수량<br></br> 드롭 쇼핑몰</h1>
            <p className="hero-description">
              정해진 시간에 열리고 재고가 소진되면 종료되는 한정 상품을 한곳에서
              확인하세요. 드롭 예정, 진행, 종료 상태를 기준으로 빠르게 판단할 수 있습니다.
            </p>
            <div className="hero-actions">
              <a href="#catalog" className="primary-button link-button">
                드롭 상품 보기
              </a>
              <Link to="/community" className="ghost-button link-button">
                드롭 후기 보기
              </Link>
            </div>
          </div>

          <dl className="hero-metrics">
            <div>
              <dt>진행 중 드롭</dt>
              <dd>{formatNumber(liveDropItems.length)}</dd>
            </div>
            <div>
              <dt>오픈 예정</dt>
              <dd>{formatNumber(upcomingDropItems.length)}</dd>
            </div>
            <div>
              <dt>구매 가능 재고</dt>
              <dd>{formatNumber(availableItemsCount)}</dd>
            </div>
          </dl>
        </div>

        <div className="hero-merch-stack">
          {heroItem ? (
            <article className="hero-feature-card">
              <div className="hero-feature-media">
                <img src={resolveImageUrl(heroItem.imageUrl)} alt={heroItem.itemName} />
              </div>
              <div className="hero-feature-copy">
                <p className="eyebrow">{dropHeroLabel ?? "FEATURED DROP"}</p>
                <h2>{heroItem.itemName}</h2>
                <p className="hero-feature-price">{formatCurrency(heroItem.price)}</p>
                <p className="muted-copy">
                  한정 재고로 운영되는 대표 상품입니다. 오픈 상태와 잔여 수량을 확인한 뒤
                  빠르게 구매 흐름으로 이동하세요.
                </p>
                <div className="hero-feature-meta">
                  <span>{dropHeroSchedule ?? getItemAvailability(heroItem.quantity)}</span>
                  <Link to={`/products/${heroItem.id}`} className="primary-button link-button">
                    드롭 상세 보기
                  </Link>
                </div>
              </div>
            </article>
          ) : (
            <article className="hero-feature-card hero-feature-card-empty">
              <div className="hero-feature-copy">
                <p className="eyebrow">NEW ARRIVAL</p>
                <h2>새로운 셀렉션을 준비 중입니다</h2>
                <p className="muted-copy">
                  곧 소개할 상품이 도착하면 이 자리에 가장 먼저 보여드릴게요.
                </p>
              </div>
            </article>
          )}

          <div className="hero-promo-grid">
            {promoCards.map((card) => {
              const hasMeta = "meta" in card;

              return (
                <article
                  key={`${card.eyebrow}-${card.title}`}
                  className={`promo-card ${hasMeta ? "promo-card-compact" : "promo-card-standard"}`}
                >
                  <p className="eyebrow">{card.eyebrow}</p>
                  <h3>{card.title}</h3>
                  {hasMeta ? (
                    <div className="promo-card-supporting">
                      <p>{card.description}</p>
                      <span className="promo-card-meta">{card.meta}</span>
                    </div>
                  ) : (
                    <p>{card.description}</p>
                  )}
                </article>
              );
            })}
          </div>
        </div>
      </section>

      <StatusBanner tone="error">{error}</StatusBanner>

      <section className="drop-command-center">
        <div className="section-header section-header-wide">
          <div>
            <p className="eyebrow">DROP BOARD</p>
            <h2>오늘의 드롭 운영 보드</h2>
            <p className="section-description">
              오픈된 상품과 대기 중인 상품을 분리해서 보여줍니다. 일반 카탈로그보다
              구매 타이밍과 잔여 수량 판단을 먼저 할 수 있게 구성했습니다.
            </p>
          </div>
          <div className="catalog-summary">
            진행 {formatNumber(liveDropItems.length)}개 · 예정 {formatNumber(upcomingDropItems.length)}개 · 종료 {formatNumber(endedDropItems.length)}개
          </div>
        </div>

        <div className="drop-board-grid">
          <section className="drop-board-panel drop-board-panel-live">
            <div className="drop-board-panel-head">
              <p className="eyebrow">OPEN NOW</p>
              <h3>지금 구매 가능한 드롭</h3>
            </div>
            <div className="drop-mini-list">
              {(liveDropItems.length > 0 ? liveDropItems : lowStockItems).slice(0, 4).map((item) => (
                <Link key={item.id} to={`/products/${item.id}`} className="drop-mini-item">
                  <img src={resolveImageUrl(item.imageUrl)} alt={item.itemName} />
                  <span>
                    <strong>{item.itemName}</strong>
                    <small>{getItemAvailability(item.quantity)} · {formatCurrency(item.price)}</small>
                  </span>
                </Link>
              ))}
              {items.length === 0 ? <p className="muted-copy">표시할 상품이 없습니다.</p> : null}
            </div>
          </section>

          <section className="drop-board-panel">
            <div className="drop-board-panel-head">
              <p className="eyebrow">UPCOMING</p>
              <h3>오픈 대기 스케줄</h3>
            </div>
            <div className="drop-schedule-list">
              {(upcomingDropItems.length > 0 ? upcomingDropItems : dropItems).slice(0, 4).map((item) => (
                <Link key={item.id} to={`/products/${item.id}`} className="drop-schedule-item">
                  <span>{getDropSaleLabel(item) ?? "드롭 상품"}</span>
                  <strong>{item.itemName}</strong>
                  <small>{getDropSaleScheduleText(item) ?? `${formatNumber(item.quantity)}점 한정`}</small>
                </Link>
              ))}
              {dropItems.length === 0 ? <p className="muted-copy">등록된 드롭 상품이 아직 없습니다.</p> : null}
            </div>
          </section>
        </div>
      </section>

      <section className="section-block">
        <div className="section-header section-header-wide">
          <div>
            <p className="eyebrow">DROP CATEGORIES</p>
            <h2>드롭 카테고리</h2>
            <p className="section-description">
              한정 상품을 카테고리별로 좁혀 봅니다. 드롭 일정과 재고 상태를 확인한 뒤
              상세 페이지에서 구매 가능 여부를 판단하세요.
            </p>
          </div>
          <div className="catalog-toolbar">
            <div className="catalog-summary">운영 카테고리 {formatNumber(categories.length)}개</div>
            <p className="field-hint">
              {selectedCategory
                ? `지금 보고 있는 카테고리: ${selectedCategory.name}`
                : "원하는 카테고리를 눌러 상품을 좁혀보세요."}
            </p>
          </div>
        </div>

        <div className="shortcut-strip">
          {shortcutCollections.map((category, index) => {
            const isActive = selectedCategoryId === category.id;

            return (
              <button
                key={category.id}
                type="button"
                className={`shortcut-card shortcut-card-media ${isActive ? "shortcut-card-active" : ""}`.trim()}
                onClick={() => handleShortcutClick(category.id)}
              >
                <div className="shortcut-card-visual">
                  {category.representativeImageUrl ? (
                    <img
                      src={resolveImageUrl(category.representativeImageUrl)}
                      alt={`${category.name} 대표 이미지`}
                      loading="lazy"
                    />
                  ) : (
                    <div className="shortcut-card-empty-copy">
                      아직 해당 카테고리에 상품이 없습니다.
                    </div>
                  )}
                </div>
                <div className="shortcut-card-body">
                  <span className="shortcut-index">{String(index + 1).padStart(2, "0")}</span>
                  <strong>{category.name}</strong>
                  <p>
                    {typeof category.itemCount === "number"
                      ? `${formatNumber(category.itemCount)}개의 상품을 만나보세요`
                      : "취향에 맞는 아이템을 빠르게 모아보세요"}
                  </p>
                  <span className="shortcut-cta">{isActive ? "전체 상품 보기" : "이 카테고리 보기"}</span>
                </div>
              </button>
            );
          })}
          {shortcutCollections.length === 0 ? (
            <div className="surface-card">
              준비 중인 카테고리입니다. 곧 새로운 상품으로 채워질 예정입니다.
            </div>
          ) : null}
        </div>
      </section>

      <section className="merchandising-grid">
        <article className="merch-story merch-story-primary">
          <p className="eyebrow">DROP LOGIC</p>
          <h2>오픈 시간, 잔여 수량, 구매 제한을 먼저 봅니다.</h2>
          <p className="muted-copy">
            일반 쇼핑몰처럼 상품을 나열하는 대신, 드롭 커머스에서 중요한 신호를
            전면에 배치했습니다. 사용자는 지금 살 수 있는지와 얼마나 남았는지를 먼저 봅니다.
          </p>
          <div className="merch-story-list">
            <div>
              <strong>오픈 상태</strong>
              <span>예정, 진행, 종료 상태를 상품 카드와 상세 화면에서 구분합니다.</span>
            </div>
            <div>
              <strong>잔여 수량</strong>
              <span>낮은 재고 상품을 별도 랭킹으로 노출해 선착순 긴장감을 만듭니다.</span>
            </div>
            <div>
              <strong>운영 화면</strong>
              <span>관리자는 상품 등록 시 드롭 시간과 1인 제한을 함께 설정합니다.</span>
            </div>
          </div>
        </article>

        <article className="merch-ranking-panel">
          <p className="eyebrow">PRICE FOCUS</p>
          <h2>가격대 상위 상품</h2>
          <ul className="merch-ranking-list">
            {premiumItems.map((item, index) => (
              <li key={item.id}>
                <Link to={`/products/${item.id}`} className="merch-ranking-item">
                  <span className="ranking-number">{String(index + 1).padStart(2, "0")}</span>
                  <div className="ranking-copy">
                    <strong>{item.itemName}</strong>
                    <span className="ranking-meta">
                      {formatCurrency(item.price)} · {getItemAvailability(item.quantity)}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
            {premiumItems.length === 0 ? (
              <li className="muted-copy">곧 추천 상품이 채워질 예정입니다.</li>
            ) : null}
          </ul>
        </article>

        <article className="merch-ranking-panel">
          <p className="eyebrow">LOW STOCK</p>
          <h2>선착순 재고 임박</h2>
          <ul className="merch-ranking-list">
            {lowStockItems.map((item, index) => (
              <li key={item.id}>
                <Link to={`/products/${item.id}`} className="merch-ranking-item">
                  <span className="ranking-number">{String(index + 1).padStart(2, "0")}</span>
                  <div className="ranking-copy">
                    <strong>{item.itemName}</strong>
                    <span className="ranking-meta">
                      {formatNumber(item.quantity)}점 남음 · {formatCurrency(item.price)}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
            {lowStockItems.length === 0 ? (
              <li className="muted-copy">인기 상품 소식이 곧 업데이트됩니다.</li>
            ) : null}
          </ul>
        </article>
      </section>

      <section id="catalog" className="section-block">
        <div className="section-header">
          <div>
            <p className="eyebrow">CATALOG</p>
            <h2>전체 드롭 카탈로그</h2>
          </div>
          <div className="catalog-summary">총 {formatNumber(visibleItems.length)}개 상품</div>
        </div>

        <div className="catalog-filter-panel">
          <div className="catalog-filter-row">
            <label className="search-shell">
              <span>카테고리</span>
              <select
                value={selectedCategoryId === null ? "all" : String(selectedCategoryId)}
                onChange={(event) =>
                  handleCategoryChange(
                    event.target.value === "all" ? null : Number(event.target.value)
                  )
                }
              >
                <option value="all">전체 카테고리</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>
            <div className="catalog-filter-search-group">
              <label className="search-shell">
                <span>검색</span>
                <input
                  ref={searchInputRef}
                  type="search"
                  placeholder="상품 이름으로 검색"
                  value={query}
                  onChange={(event) => handleQueryChange(event.target.value)}
                />
              </label>
              <button
                type="button"
                className="ghost-button"
                onClick={() => {
                  setQuery("");
                  handleCategoryChange(null);
                }}
              >
                필터 초기화
              </button>
            </div>
          </div>
        </div>

        <div
          ref={catalogResultsRef}
          className="catalog-results"
          style={
            catalogResultsMinHeight === null
              ? undefined
              : {
                  minHeight: `${catalogResultsMinHeight}px`
                }
          }
        >
          {loading || catalogLoading ? (
            <div className="surface-card">상품을 준비하는 중입니다.</div>
          ) : null}

          {!loading && !catalogLoading && visibleItems.length === 0 ? (
            <div className="surface-card">
              선택한 조건에 맞는 드롭 상품이 없습니다. 다른 검색어나 카테고리로 다시 살펴보세요.
            </div>
          ) : null}

          <div className="product-grid">
            {visibleItems.map((item) => (
              <ProductCard key={item.id} item={item} />
            ))}
          </div>
        </div>
      </section>

      <section className="section-block community-preview">
        <div className="section-header">
          <div>
            <p className="eyebrow">DROP COMMUNITY</p>
            <h2>드롭 후기 피드</h2>
            <p className="section-description">
              구매 성공 후기, 오픈 대기 경험, 사이즈 팁을 함께 나누는 공간입니다.
            </p>
          </div>
          <Link to="/community" className="ghost-button link-button">
            피드 전체 보기
          </Link>
        </div>

        <div className="community-preview-grid">
          {communityHighlights.map((post, index) => (
            <Link
              key={post.id}
              to={`/community/${post.id}`}
              className={`community-card ${index === 0 ? "community-card-featured" : ""}`.trim()}
            >
              <span className="eyebrow">FROM {post.author}</span>
              <h3>{post.title}</h3>
              <p>{post.content}</p>
              <span className="community-meta">{formatDateTime(post.createdDate)}</span>
            </Link>
          ))}

          {posts.length === 0 ? (
            <div className="surface-card">
              아직 등록된 이야기가 없습니다. 첫 스타일 후기를 남겨 보세요.
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
