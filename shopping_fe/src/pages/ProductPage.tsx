import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  fetchItem,
  fetchItems,
  isUnauthorizedError,
  toAppErrorMessage
} from "../api/client";
import type { Item, ItemDetailImage } from "../api/types";
import { ProductCard } from "../components/ProductCard";
import { QuantityField } from "../components/QuantityField";
import { StatusBanner } from "../components/StatusBanner";
import { useCart } from "../contexts/CartContext";
import {
  getDropSaleLabel,
  getDropSaleScheduleText,
  getDropSaleStatus
} from "../lib/drop";
import {
  formatCurrency,
  formatNumber,
  getItemAvailability,
  resolveImageUrl
} from "../lib/format";

interface DetailCopy {
  pointTitle: string;
  pointBody: string;
  materialTitle: string;
  materialBody: string;
  detailBullets: string[];
  sizeHeaders: string[];
  sizeRows: string[][];
}

interface GalleryImage {
  src: string;
  alt: string;
  label: string;
  itemName: string;
}

interface MaterialImage {
  src: string;
  alt: string;
  caption: string;
}

const DEFAULT_DETAIL_COPY: DetailCopy = {
  pointTitle: "드롭 타이밍에 맞춘 한정 셀렉션",
  pointBody:
    "과한 장식보다 매일 쓰기 좋은 형태와 안정적인 색감을 중심으로 고른 상품입니다. 수량이 제한된 드롭 상품이라 오픈 시간과 잔여 재고를 함께 확인하는 흐름에 맞춰 구성했습니다.",
  materialTitle: "데일리 사용에 맞춘 기본 사양",
  materialBody:
    "외부 활동과 반복 사용을 고려해 관리가 쉬운 소재, 안정적인 마감, 오래 봐도 부담 없는 비율을 중심으로 소개합니다.",
  detailBullets: [
    "대표 이미지는 상품의 전체 실루엣을 확인하는 용도입니다.",
    "무드 이미지는 같은 카테고리 상품을 활용해 스타일 방향을 함께 보여줍니다.",
    "실측은 상품군별 참고 가이드이며 실제 측정 방식에 따라 약간의 차이가 있을 수 있습니다."
  ],
  sizeHeaders: ["사이즈", "가로", "세로", "추천"],
  sizeRows: [
    ["S", "44cm", "62cm", "슬림"],
    ["M", "48cm", "66cm", "레귤러"],
    ["L", "52cm", "70cm", "여유"]
  ]
};

const CATEGORY_DETAIL_COPY: Record<string, DetailCopy> = {
  신발: {
    pointTitle: "발끝부터 빠르게 완성되는 드롭 룩",
    pointBody:
      "착화감과 실루엣을 동시에 볼 수 있도록 스니커즈 중심의 무드 컷을 배치했습니다. 데님, 조거, 쇼츠와 매칭했을 때도 균형이 무너지지 않는 상품 흐름을 보여줍니다.",
    materialTitle: "어퍼, 아웃솔, 쿠션감",
    materialBody:
      "메쉬, 합성가죽, 러버 솔처럼 자주 쓰이는 신발 디테일을 텍스트와 이미지로 나눠 보여줘 구매 전 확인해야 할 포인트를 빠르게 읽을 수 있습니다.",
    detailBullets: [
      "정사이즈 기준으로 비교하되 발볼이 넓다면 반 사이즈 업을 고려하세요.",
      "아웃솔 높이와 소재감은 착화 이미지에서 먼저 확인할 수 있게 구성했습니다.",
      "드롭 오픈 직후 사이즈별 품절이 빠를 수 있어 수량 선택을 간결하게 유지했습니다."
    ],
    sizeHeaders: ["사이즈", "발길이", "굽", "추천 핏"],
    sizeRows: [
      ["250", "250mm", "3cm", "정사이즈"],
      ["260", "260mm", "3cm", "정사이즈"],
      ["270", "270mm", "3cm", "여유 핏"]
    ]
  },
  가방: {
    pointTitle: "수납과 실루엣을 같이 보는 가방 상세",
    pointBody:
      "한 장의 대표 사진으로는 알기 어려운 크기감과 스타일링 방향을 무드 컷으로 보강했습니다. 일상 수납, 출퇴근, 주말 외출에 어울리는 균형을 보여줍니다.",
    materialTitle: "원단, 스트랩, 수납부",
    materialBody:
      "겉감의 질감과 스트랩 폭, 메인 수납부의 구조를 짧은 문장으로 정리해 사진만 보고 놓치기 쉬운 디테일을 보완합니다.",
    detailBullets: [
      "무드 컷으로 착용 위치와 실제 크기감을 먼저 확인할 수 있습니다.",
      "스트랩 조절 가능 여부와 내부 수납 구조를 구매 체크 포인트로 분리했습니다.",
      "데일리 사용성을 기준으로 무게감과 관리 난이도를 함께 설명합니다."
    ],
    sizeHeaders: ["사이즈", "가로", "세로", "폭"],
    sizeRows: [
      ["MINI", "22cm", "16cm", "7cm"],
      ["MEDIUM", "32cm", "24cm", "11cm"],
      ["LARGE", "42cm", "30cm", "14cm"]
    ]
  },
  모자: {
    pointTitle: "얼굴형과 스타일을 가볍게 잡아주는 포인트",
    pointBody:
      "모자는 정면 사진보다 착용 높이, 챙 길이, 깊이감이 중요합니다. 무드 컷을 통해 전체 코디에서 어느 정도의 존재감을 갖는지 먼저 볼 수 있게 했습니다.",
    materialTitle: "챙, 크라운, 둘레 조절",
    materialBody:
      "챙의 단단함과 크라운 깊이, 뒤쪽 조절 장치처럼 구매 전에 확인해야 할 디테일을 작은 이미지와 함께 정리합니다.",
    detailBullets: [
      "FREE 사이즈라도 머리둘레와 깊이감은 상품마다 다를 수 있습니다.",
      "챙 길이는 얼굴형과 코디 분위기에 큰 영향을 줍니다.",
      "밝은 색상은 생활 오염 관리 방법을 함께 확인하는 편이 좋습니다."
    ],
    sizeHeaders: ["사이즈", "머리둘레", "챙 길이", "깊이"],
    sizeRows: [
      ["FREE", "56-60cm", "7cm", "보통"],
      ["DEEP", "57-61cm", "7.5cm", "깊음"],
      ["LOW", "55-59cm", "6.5cm", "얕음"]
    ]
  },
  시계: {
    pointTitle: "손목 위에서 보이는 비율 중심 상세",
    pointBody:
      "시계는 제품 단독 사진보다 케이스 크기와 밴드 폭이 손목에서 어떻게 보이는지가 중요합니다. 무드 컷으로 착용 비율과 스타일 방향을 함께 확인합니다.",
    materialTitle: "케이스, 밴드, 글래스",
    materialBody:
      "케이스 소재와 밴드 타입, 글래스 마감처럼 착용감과 관리에 영향을 주는 요소를 상세 섹션에서 분리해 보여줍니다.",
    detailBullets: [
      "케이스 직경은 손목 두께에 따라 체감 크기가 달라집니다.",
      "가죽, 메탈, 실리콘 밴드는 관리 방식이 서로 다릅니다.",
      "생활 방수 표기는 수영이나 샤워 사용 가능 여부와 다를 수 있습니다."
    ],
    sizeHeaders: ["사이즈", "케이스", "밴드 폭", "무게"],
    sizeRows: [
      ["SMALL", "36mm", "18mm", "42g"],
      ["MEDIUM", "40mm", "20mm", "56g"],
      ["LARGE", "44mm", "22mm", "72g"]
    ]
  },
  키즈: {
    pointTitle: "활동량을 고려한 키즈 드롭 구성",
    pointBody:
      "키즈 상품은 예쁜 사진만큼 움직임, 세탁, 사이즈 여유가 중요합니다. 착용/무드 섹션에서 활동적인 장면과 실용적인 구매 포인트를 함께 보여줍니다.",
    materialTitle: "부드러운 촉감과 쉬운 관리",
    materialBody:
      "아이들이 입고 움직이는 상황을 고려해 피부에 닿는 소재, 세탁 편의성, 여밈 구조를 빠르게 확인할 수 있게 정리합니다.",
    detailBullets: [
      "성장 속도를 고려해 한 치수 여유 있는 선택이 필요할 수 있습니다.",
      "세탁 빈도가 높은 상품군이라 관리 방법을 먼저 확인하는 편이 좋습니다.",
      "작은 부자재가 있는 상품은 보호자 확인 후 착용을 권장합니다."
    ],
    sizeHeaders: ["사이즈", "권장 키", "가슴", "추천 연령"],
    sizeRows: [
      ["110", "105-115cm", "58cm", "4-5세"],
      ["120", "115-125cm", "62cm", "6-7세"],
      ["130", "125-135cm", "66cm", "8-9세"]
    ]
  },
  액세서리: {
    pointTitle: "작지만 룩의 방향을 바꾸는 액세서리",
    pointBody:
      "액세서리는 크기, 광택, 착용 위치에 따라 분위기가 크게 달라집니다. 대표 사진과 무드 컷을 이어서 보여줘 실제 코디에서의 밀도를 확인합니다.",
    materialTitle: "도금, 마감, 착용 방식",
    materialBody:
      "소재 표기와 마감 방식, 잠금 구조처럼 작은 상품에서 중요한 정보를 텍스트와 이미지로 분리해 읽기 쉽게 구성합니다.",
    detailBullets: [
      "금속 알레르기가 있다면 소재 표기를 먼저 확인해야 합니다.",
      "광택감은 조명과 화면 환경에 따라 다르게 보일 수 있습니다.",
      "보관 시 마찰과 습기를 줄이면 표면 손상을 줄일 수 있습니다."
    ],
    sizeHeaders: ["사이즈", "길이", "폭", "무게"],
    sizeRows: [
      ["S", "16cm", "0.4cm", "8g"],
      ["M", "18cm", "0.5cm", "10g"],
      ["L", "20cm", "0.6cm", "12g"]
    ]
  },
  아우터: {
    pointTitle: "첫인상을 결정하는 아우터 실루엣",
    pointBody:
      "아우터는 전체 코디의 면적을 크게 차지하므로 길이감, 어깨선, 여밈 구조가 중요합니다. 착용/무드 컷으로 실제 비율을 먼저 보여줍니다.",
    materialTitle: "겉감, 안감, 여밈 디테일",
    materialBody:
      "겉감 질감과 안감 유무, 지퍼나 버튼의 마감처럼 체감 품질을 좌우하는 정보를 상세 섹션에서 따로 정리합니다.",
    detailBullets: [
      "이너 두께를 고려해 가슴 단면과 어깨선을 확인하세요.",
      "총장은 키에 따라 체감 비율이 달라지는 핵심 수치입니다.",
      "드롭 상품은 오픈 직후 인기 사이즈부터 빠르게 소진될 수 있습니다."
    ],
    sizeHeaders: ["사이즈", "어깨", "가슴", "총장"],
    sizeRows: [
      ["S", "44cm", "54cm", "64cm"],
      ["M", "46cm", "57cm", "67cm"],
      ["L", "48cm", "60cm", "70cm"]
    ]
  }
};

function getDetailCopy(categoryName?: string | null): DetailCopy {
  if (!categoryName) {
    return DEFAULT_DETAIL_COPY;
  }

  return CATEGORY_DETAIL_COPY[categoryName.trim()] ?? DEFAULT_DETAIL_COPY;
}

function getCategoryLabel(item: Item): string {
  return item.categoryName ?? "드롭 셀렉션";
}

function getSortedDetailImages(item: Item, imageRole: ItemDetailImage["imageRole"]): ItemDetailImage[] {
  return (item.detailImages ?? [])
    .filter((detailImage) => detailImage.imageRole === imageRole)
    .sort((first, second) => first.displayOrder - second.displayOrder);
}

function buildGalleryImages(item: Item, relatedItems: Item[]): GalleryImage[] {
  const moodImages = getSortedDetailImages(item, "MOOD");
  const galleryImages = moodImages.slice(0, 2).map((detailImage, index) => ({
    src: resolveImageUrl(detailImage.imageUrl),
    alt: detailImage.altText,
    label: detailImage.caption ?? (index === 0 ? "착용 컷" : "무드 컷"),
    itemName: item.itemName
  }));

  const seen = new Set<number>();
  const candidates = [item, ...relatedItems].filter((candidate) => {
    if (seen.has(candidate.id)) {
      return false;
    }

    seen.add(candidate.id);
    return true;
  });

  for (const candidate of candidates.slice(1)) {
    if (galleryImages.length >= 2) {
      break;
    }

    galleryImages.push({
      src: resolveImageUrl(candidate.imageUrl),
      alt: `${candidate.itemName} ${galleryImages.length === 0 ? "착용" : "무드"} 이미지`,
      label: galleryImages.length === 0 ? "착용 컷" : "무드 컷",
      itemName: candidate.itemName
    });
  }

  while (galleryImages.length < 2) {
    galleryImages.push({
      src: resolveImageUrl(item.imageUrl),
      alt: `${item.itemName} ${galleryImages.length === 0 ? "착용" : "무드"} 이미지`,
      label: galleryImages.length === 0 ? "착용 컷" : "무드 컷",
      itemName: item.itemName
    });
  }

  return galleryImages;
}

function getMaterialImage(item: Item, relatedItems: Item[]): MaterialImage {
  const detailImage = getSortedDetailImages(item, "DETAIL")[0];

  if (detailImage) {
    return {
      src: resolveImageUrl(detailImage.imageUrl),
      alt: detailImage.altText,
      caption: detailImage.caption ?? item.itemName
    };
  }

  const fallbackItem = relatedItems[2] ?? relatedItems[0] ?? item;

  return {
    src: resolveImageUrl(fallbackItem.imageUrl),
    alt: `${fallbackItem.itemName} 소재 디테일 이미지`,
    caption: fallbackItem.itemName
  };
}

function ProductMoodSection({
  item,
  relatedItems,
  detailCopy
}: {
  item: Item;
  relatedItems: Item[];
  detailCopy: DetailCopy;
}) {
  const galleryImages = buildGalleryImages(item, relatedItems);

  return (
    <section className="section-block detail-editorial-section">
      <div className="section-header section-header-wide">
        <div>
          <p className="eyebrow">MOOD & FIT</p>
          <h2>착용/무드 컷</h2>
          <p className="section-description">{detailCopy.pointBody}</p>
        </div>
      </div>
      <div className="detail-mood-grid">
        {galleryImages.map((galleryImage) => (
          <figure className="detail-mood-card" key={`${galleryImage.label}-${galleryImage.itemName}`}>
            <img src={galleryImage.src} alt={galleryImage.alt} loading="lazy" />
            <figcaption>
              <span>{galleryImage.label}</span>
              <strong>{galleryImage.itemName}</strong>
            </figcaption>
          </figure>
        ))}
        <article className="detail-point-panel">
          <span className="detail-pill">{getCategoryLabel(item)}</span>
          <h3>{detailCopy.pointTitle}</h3>
          <p>
            {item.itemName}은 현재 상세 페이지의 중심 상품이고, 상품별 상세 이미지와 추천
            이미지를 함께 배치해 실제 쇼핑몰 상세처럼 스타일 방향까지 이어서 확인할 수 있게 했습니다.
          </p>
          <dl className="detail-feature-list">
            <div>
              <dt>대표 가격</dt>
              <dd>{formatCurrency(item.price)}</dd>
            </div>
            <div>
              <dt>남은 수량</dt>
              <dd>{formatNumber(item.quantity)}점</dd>
            </div>
            <div>
              <dt>카테고리</dt>
              <dd>{getCategoryLabel(item)}</dd>
            </div>
          </dl>
        </article>
      </div>
    </section>
  );
}

function ProductMaterialSection({
  item,
  relatedItems,
  detailCopy
}: {
  item: Item;
  relatedItems: Item[];
  detailCopy: DetailCopy;
}) {
  const materialImage = getMaterialImage(item, relatedItems);

  return (
    <section className="section-block">
      <div className="detail-info-grid">
        <article className="detail-material-panel">
          <div className="detail-material-copy">
            <p className="eyebrow">MATERIAL & DETAIL</p>
            <h2>소재와 디테일</h2>
            <h3>{detailCopy.materialTitle}</h3>
            <p>{detailCopy.materialBody}</p>
          </div>
          <figure className="detail-material-media">
            <img
              src={materialImage.src}
              alt={materialImage.alt}
              loading="lazy"
            />
            <figcaption>{materialImage.caption}</figcaption>
          </figure>
        </article>

        <article className="detail-check-panel">
          <p className="eyebrow">BUYING POINT</p>
          <h2>구매 전 체크</h2>
          <ul className="detail-bullet-list">
            {detailCopy.detailBullets.map((detailBullet) => (
              <li key={detailBullet}>{detailBullet}</li>
            ))}
          </ul>
        </article>
      </div>
    </section>
  );
}

function ProductSizeGuideSection({ detailCopy }: { detailCopy: DetailCopy }) {
  return (
    <article className="detail-size-panel">
      <p className="eyebrow">SIZE GUIDE</p>
      <h2>사이즈 가이드</h2>
      <div className="size-guide-scroll">
        <table className="size-guide-table">
          <caption>상품군 기준 참고 실측</caption>
          <thead>
            <tr>
              {detailCopy.sizeHeaders.map((header) => (
                <th scope="col" key={header}>
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {detailCopy.sizeRows.map((row) => (
              <tr key={row.join("-")}>
                {row.map((cell) => (
                  <td key={cell}>{cell}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </article>
  );
}

function ProductServiceInfoSection() {
  return (
    <article className="detail-policy-panel">
      <p className="eyebrow">SERVICE</p>
      <h2>배송/교환 안내</h2>
      <div className="detail-accordion">
        <details open>
          <summary>배송 안내</summary>
          <p>
            결제 완료 후 영업일 기준 1-3일 안에 출고됩니다. 드롭 상품은 주문량이 몰리면
            출고 순서가 나뉠 수 있습니다.
          </p>
        </details>
        <details>
          <summary>교환/반품 안내</summary>
          <p>
            상품 수령 후 7일 이내 접수할 수 있습니다. 착용 흔적, 택 제거, 구성품 누락이
            있으면 교환/반품이 제한될 수 있습니다.
          </p>
        </details>
      </div>
    </article>
  );
}

export function ProductPage() {
  const params = useParams();
  const navigate = useNavigate();
  const { addItem } = useCart();
  const [item, setItem] = useState<Item | null>(null);
  const [relatedItems, setRelatedItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    const itemId = Number(params.productId);

    if (!itemId) {
      setFeedback("잘못된 상품 경로입니다.");
      setLoading(false);
      return;
    }

    let cancelled = false;

    async function load() {
      setLoading(true);
      setFeedback(null);

      try {
        const [nextItem, allItems] = await Promise.all([
          fetchItem(itemId),
          fetchItems()
        ]);

        if (cancelled) {
          return;
        }

        const nextRelatedItems = allItems
          .filter((entry) => entry.id !== itemId)
          .filter((entry) =>
            nextItem.categoryId === null || nextItem.categoryId === undefined
              ? false
              : entry.categoryId === nextItem.categoryId
          )
          .slice(0, 3);

        setItem(nextItem);
        setQuantity(nextItem.quantity > 0 ? 1 : 0);
        setRelatedItems(nextRelatedItems);
      } catch (error) {
        if (!cancelled) {
          setFeedback(toAppErrorMessage(error, "상품 정보를 불러오지 못했습니다."));
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
  }, [params.productId]);

  async function handleAddToCart() {
    if (!item) {
      return;
    }

    try {
      await addItem(item.id, quantity);
      setFeedback(`${item.itemName} ${quantity}개를 장바구니에 담았습니다.`);
      navigate("/cart");
    } catch (error) {
      if (isUnauthorizedError(error)) {
        return;
      }

      setFeedback(toAppErrorMessage(error));
    }
  }

  if (loading) {
    return <div className="surface-card">상품을 준비하는 중입니다.</div>;
  }

  if (!item) {
    return (
      <div className="page-stack">
        <StatusBanner tone="error">{feedback ?? "상품을 찾을 수 없습니다."}</StatusBanner>
        <Link to="/" className="ghost-button link-button">
          홈으로 돌아가기
        </Link>
      </div>
    );
  }

  const dropLabel = getDropSaleLabel(item);
  const dropSchedule = getDropSaleScheduleText(item);
  const dropStatus = getDropSaleStatus(item);
  const canAddToCart = item.quantity > 0 && (!item.dropProduct || dropStatus === "LIVE");
  const detailCopy = getDetailCopy(item.categoryName);

  return (
    <div className="page-stack">
      <StatusBanner tone="info">{feedback}</StatusBanner>

      <section className="product-detail-shell">
        <div className="product-detail-media">
          <img src={resolveImageUrl(item.imageUrl)} alt={item.itemName} />
        </div>
        <div className="product-detail-copy">
          <p className="eyebrow">PRODUCT DETAIL</p>
          <h1>{item.itemName}</h1>
          {dropLabel ? <p className="drop-sale-badge">{dropLabel}</p> : null}
          <p className="detail-price">{formatCurrency(item.price)}</p>
          <p className="detail-stock">{getItemAvailability(item.quantity)}</p>
          {dropSchedule ? <p className="field-hint">{dropSchedule}</p> : null}
          <p className="detail-description">
            정해진 시간에 열리는 한정 수량 상품입니다. 오픈 상태, 잔여 재고, 1인 구매
            제한을 확인한 뒤 구매 가능한 시점에 장바구니에 담을 수 있습니다.
          </p>

          <dl className="detail-stats">
            <div>
              <dt>상품 번호</dt>
              <dd>#{item.id}</dd>
            </div>
            <div>
              <dt>재고</dt>
              <dd>{formatNumber(item.quantity)}점</dd>
            </div>
            <div>
              <dt>드롭 오픈 시간</dt>
              <dd>{dropSchedule ?? "상시 판매"}</dd>
            </div>
            <div>
              <dt>상태</dt>
              <dd>{dropLabel ?? (item.quantity > 0 ? "판매중" : "품절")}</dd>
            </div>
            {item.dropProduct ? (
              <div>
                <dt>1인 제한</dt>
                <dd>{formatNumber(item.dropPurchaseLimit ?? 1)}개</dd>
              </div>
            ) : null}
          </dl>

          {item.dropProduct ? (
            <div className="drop-purchase-note">
              <strong>
                {dropStatus === "LIVE"
                  ? "지금 구매 가능한 드롭입니다."
                  : dropStatus === "UPCOMING"
                    ? "아직 오픈 전입니다."
                    : "종료된 드롭입니다."}
              </strong>
              <p>
                {dropStatus === "LIVE"
                  ? "구매 요청이 몰릴 수 있으니 수량을 확인하고 바로 진행하세요."
                  : dropStatus === "UPCOMING"
                    ? "오픈 시간이 되면 장바구니 버튼이 활성화됩니다."
                    : "종료된 상품은 재오픈 전까지 구매할 수 없습니다."}
              </p>
            </div>
          ) : null}

          <div className="purchase-panel">
            <QuantityField
              value={quantity || 1}
              min={1}
              max={Math.max(item.quantity, 1)}
              onChange={setQuantity}
            />
            <button
              type="button"
              className="primary-button"
              onClick={() => void handleAddToCart()}
              disabled={!canAddToCart}
            >
              {canAddToCart ? "장바구니에 담기" : item.quantity <= 0 ? "품절" : "드롭 오픈 대기"}
            </button>
          </div>
        </div>
      </section>

      <ProductMoodSection item={item} relatedItems={relatedItems} detailCopy={detailCopy} />

      <ProductMaterialSection item={item} relatedItems={relatedItems} detailCopy={detailCopy} />

      <section className="section-block">
        <div className="detail-service-grid">
          <ProductSizeGuideSection detailCopy={detailCopy} />
          <ProductServiceInfoSection />
        </div>
      </section>

      <section className="section-block">
        <div className="section-header">
          <div>
            <p className="eyebrow">SAME CATEGORY</p>
            <h2>추천 상품</h2>
          </div>
        </div>
        <div className="product-grid">
          {relatedItems.length > 0 ? (
            relatedItems.map((relatedItem) => (
              <ProductCard key={relatedItem.id} item={relatedItem} />
            ))
          ) : (
            <p className="empty-copy">같은 카테고리의 다른 상품이 아직 없습니다.</p>
          )}
        </div>
      </section>
    </div>
  );
}
