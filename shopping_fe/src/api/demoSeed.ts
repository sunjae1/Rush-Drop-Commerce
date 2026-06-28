import type { Item, User } from "./types";

export interface DemoSeedUser extends User {
  password: string;
}

export interface DemoSeedCategory {
  id: number;
  name: string;
}

export interface DemoSeedCartEntry {
  itemId: number;
  quantity: number;
}

export interface DemoSeedOrderLineItem {
  itemId: number;
  itemName: string;
  price: number;
  quantity: number;
}

export interface DemoSeedOrder {
  id: number;
  userId: number;
  orderDate: string;
  status: string;
  lineItems: DemoSeedOrderLineItem[];
}

export interface DemoSeedComment {
  id: number;
  userId: number;
  username: string;
  content: string;
  createdDate: string;
}

export interface DemoSeedPost {
  id: number;
  authorId: number;
  author: string;
  title: string;
  content: string;
  createdDate: string;
  comments: DemoSeedComment[];
}

export interface DemoStoreSeed {
  users: DemoSeedUser[];
  categories: DemoSeedCategory[];
  items: Item[];
  carts: Record<string, DemoSeedCartEntry[]>;
  orders: DemoSeedOrder[];
  posts: DemoSeedPost[];
  nextIds: {
    user: number;
    category: number;
    item: number;
    order: number;
    post: number;
    comment: number;
  };
}

const demoImageUrls = {
  graphicTee:
    "https://images.pexels.com/photos/33258835/pexels-photo-33258835.jpeg?auto=compress&cs=tinysrgb&w=1200",
  leatherJacket:
    "https://images.pexels.com/photos/12148300/pexels-photo-12148300.jpeg?auto=compress&cs=tinysrgb&w=1200",
  trenchCoat:
    "https://images.pexels.com/photos/9968540/pexels-photo-9968540.jpeg?auto=compress&cs=tinysrgb&w=1200",
  knitSweater:
    "https://images.pexels.com/photos/14463985/pexels-photo-14463985.jpeg?auto=compress&cs=tinysrgb&w=1200",
  denimJeans:
    "https://images.pexels.com/photos/4109798/pexels-photo-4109798.jpeg?auto=compress&cs=tinysrgb&w=1200",
  whiteSneakers:
    "https://images.pexels.com/photos/4252969/pexels-photo-4252969.jpeg?auto=compress&cs=tinysrgb&w=1200",
  blackHoodie:
    "https://images.pexels.com/photos/35408208/pexels-photo-35408208.jpeg?auto=compress&cs=tinysrgb&w=1200",
  runnerSneakers:
    "https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg?auto=compress&cs=tinysrgb&w=1200",
  toteBag:
    "https://images.pexels.com/photos/36367484/pexels-photo-36367484.jpeg?auto=compress&cs=tinysrgb&w=1200",
  yellowCap:
    "https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1200",
  classicWatch:
    "https://images.pexels.com/photos/9713527/pexels-photo-9713527.jpeg?auto=compress&cs=tinysrgb&w=1200",
  kidsSet:
    "https://images.pexels.com/photos/37451174/pexels-photo-37451174.jpeg?auto=compress&cs=tinysrgb&w=1200",
  accessorySet:
    "https://images.pexels.com/photos/2986445/pexels-photo-2986445.jpeg?auto=compress&cs=tinysrgb&w=1200"
};

interface AdditionalDemoProduct {
  itemName: string;
  price: number;
  quantity: number;
  categoryId: number;
  categoryName: string;
  imageUrl: string;
}

const additionalDropMarketProducts: AdditionalDemoProduct[] = [
  {
    itemName: "라이트 코트 스니커즈",
    price: 79000,
    quantity: 12,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/15398044/pexels-photo-15398044.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "파스텔 러닝 스니커즈",
    price: 99000,
    quantity: 8,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/29548615/pexels-photo-29548615.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "크로스워크 캔버스 슈즈",
    price: 69000,
    quantity: 13,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/3375910/pexels-photo-3375910.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "블랙 하이탑 스니커즈",
    price: 86000,
    quantity: 9,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/9333372/pexels-photo-9333372.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "컬러 콘크리트 스니커즈",
    price: 92000,
    quantity: 7,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "데일리 화이트 스니커즈",
    price: 76000,
    quantity: 14,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/15398044/pexels-photo-15398044.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "어반 퍼플 러너",
    price: 97000,
    quantity: 8,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/29548615/pexels-photo-29548615.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "모노 캔버스 슈즈",
    price: 72000,
    quantity: 11,
    categoryId: 4,
    categoryName: "신발",
    imageUrl: "https://images.pexels.com/photos/9333372/pexels-photo-9333372.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "클래식 레더 토트백",
    price: 129000,
    quantity: 8,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/29096395/pexels-photo-29096395.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "미니 체인 숄더백",
    price: 99000,
    quantity: 10,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/34997535/pexels-photo-34997535.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "에브리데이 캔버스 숄더백",
    price: 69000,
    quantity: 15,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/34027219/pexels-photo-34027219.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "시티 버클 크로스백",
    price: 89000,
    quantity: 11,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/29793778/pexels-photo-29793778.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "소프트 호보백",
    price: 119000,
    quantity: 7,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/36364966/pexels-photo-36364966.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "슬림 메신저백",
    price: 109000,
    quantity: 9,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/36492563/pexels-photo-36492563.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "골드 이브닝 백",
    price: 139000,
    quantity: 6,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/34965306/pexels-photo-34965306.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "트래블 위켄더 백",
    price: 149000,
    quantity: 5,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/29096395/pexels-photo-29096395.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "베이지 데일리 숄더백",
    price: 96000,
    quantity: 12,
    categoryId: 5,
    categoryName: "가방",
    imageUrl: "https://images.pexels.com/photos/34027219/pexels-photo-34027219.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "블루 로고 볼캡",
    price: 42000,
    quantity: 12,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/13697753/pexels-photo-13697753.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "스토어 그래픽 캡",
    price: 45000,
    quantity: 9,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/31162881/pexels-photo-31162881.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "퍼플 부티크 캡",
    price: 41000,
    quantity: 11,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/18313293/pexels-photo-18313293.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "컬러 셸프 볼캡",
    price: 43000,
    quantity: 13,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/13697753/pexels-photo-13697753.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "레드 포인트 캡",
    price: 39000,
    quantity: 14,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "블랙 트러커 캡",
    price: 46000,
    quantity: 8,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/31162881/pexels-photo-31162881.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "라벤더 데일리 캡",
    price: 40000,
    quantity: 12,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/18313293/pexels-photo-18313293.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "그린 플랜트 볼캡",
    price: 44000,
    quantity: 9,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/13697753/pexels-photo-13697753.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "선글라스 세트 볼캡",
    price: 49000,
    quantity: 7,
    categoryId: 6,
    categoryName: "모자",
    imageUrl: "https://images.pexels.com/photos/14868398/pexels-photo-14868398.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "헤리티지 브라운 스트랩 워치",
    price: 189000,
    quantity: 7,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/9395501/pexels-photo-9395501.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "클래식 골드 다이얼 워치",
    price: 219000,
    quantity: 5,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/11780439/pexels-photo-11780439.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "스톤 클래식 레더 워치",
    price: 199000,
    quantity: 8,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/34031837/pexels-photo-34031837.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "포레스트 브라운 워치",
    price: 179000,
    quantity: 9,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/9381643/pexels-photo-9381643.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "블랙 크로노 워치",
    price: 249000,
    quantity: 4,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/3083461/pexels-photo-3083461.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "로즈 핑크 레더 워치",
    price: 169000,
    quantity: 10,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/11780439/pexels-photo-11780439.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "두바이 클래식 워치",
    price: 239000,
    quantity: 6,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/10347090/pexels-photo-10347090.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "문페이즈 드레스 워치",
    price: 279000,
    quantity: 3,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/13548994/pexels-photo-13548994.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "다크 클래식 워치",
    price: 189000,
    quantity: 7,
    categoryId: 7,
    categoryName: "시계",
    imageUrl: "https://images.pexels.com/photos/6349111/pexels-photo-6349111.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 데일리 후디 세트",
    price: 59000,
    quantity: 14,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/28808342/pexels-photo-28808342.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 라이트 윈드브레이커",
    price: 69000,
    quantity: 10,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/34608858/pexels-photo-34608858.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 컬러 선글라스 티셔츠",
    price: 39000,
    quantity: 18,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/35338045/pexels-photo-35338045.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 스포츠 저지 세트",
    price: 52000,
    quantity: 16,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/35338044/pexels-photo-35338044.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 파스텔 원피스",
    price: 64000,
    quantity: 9,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/14622860/pexels-photo-14622860.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 소프트 맨투맨",
    price: 43000,
    quantity: 17,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/5560018/pexels-photo-5560018.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 스트리트 점퍼",
    price: 71000,
    quantity: 8,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/33572819/pexels-photo-33572819.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 플레이웨어 세트",
    price: 47000,
    quantity: 15,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/30110229/pexels-photo-30110229.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "키즈 위켄드 팬츠",
    price: 41000,
    quantity: 19,
    categoryId: 8,
    categoryName: "키즈",
    imageUrl: "https://images.pexels.com/photos/34181925/pexels-photo-34181925.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "럭셔리 링 세트",
    price: 79000,
    quantity: 10,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/29986281/pexels-photo-29986281.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "빈티지 펄 네크리스",
    price: 89000,
    quantity: 7,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/36229132/pexels-photo-36229132.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "터콰이즈 비즈 브레이슬릿",
    price: 45000,
    quantity: 16,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/10596294/pexels-photo-10596294.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "골드 이어링 포인트",
    price: 68000,
    quantity: 11,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/32989031/pexels-photo-32989031.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "비즈 레이어드 네크리스",
    price: 59000,
    quantity: 13,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/20257348/pexels-photo-20257348.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "사파이어 무드 이어링",
    price: 72000,
    quantity: 9,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/29193423/pexels-photo-29193423.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "골드 젬 브레이슬릿",
    price: 95000,
    quantity: 6,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/29245551/pexels-photo-29245551.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "마켓 빈티지 주얼리 믹스",
    price: 52000,
    quantity: 18,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/5602433/pexels-photo-5602433.jpeg?auto=compress&cs=tinysrgb&w=1200"
  },
  {
    itemName: "레진 팬던트 네크리스",
    price: 47000,
    quantity: 14,
    categoryId: 9,
    categoryName: "액세서리",
    imageUrl: "https://images.pexels.com/photos/17225139/pexels-photo-17225139.jpeg?auto=compress&cs=tinysrgb&w=1200"
  }
];

export function createDemoSeed(): DemoStoreSeed {
  const liveDropStartsAt = new Date(Date.now() - 30 * 60 * 1000).toISOString();
  const liveDropEndsAt = new Date(Date.now() + 90 * 60 * 1000).toISOString();
  const upcomingDropStartsAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();
  const upcomingDropEndsAt = new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString();
  const endedDropStartsAt = new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString();
  const endedDropEndsAt = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString();

  return {
    users: [
      {
        id: 1,
        email: "demo@seoulselect.com",
        name: "서연",
        role: "USER",
        password: "demo123!"
      },
      {
        id: 2,
        email: "admin@seoulselect.com",
        name: "운영자",
        role: "ADMIN",
        password: "admin123!"
      },
      {
        id: 3,
        email: "mira@seoulselect.com",
        name: "미라",
        role: "USER",
        password: "style123!"
      }
    ],
    categories: [
      {
        id: 1,
        name: "아우터"
      },
      {
        id: 2,
        name: "상의"
      },
      {
        id: 3,
        name: "하의"
      },
      {
        id: 4,
        name: "신발"
      },
      {
        id: 5,
        name: "가방"
      },
      {
        id: 6,
        name: "모자"
      },
      {
        id: 7,
        name: "시계"
      },
      {
        id: 8,
        name: "키즈"
      },
      {
        id: 9,
        name: "액세서리"
      }
    ],
    items: [
      {
        id: 1,
        itemName: "드롭 그래픽 티셔츠",
        price: 29000,
        quantity: 12,
        categoryId: 2,
        categoryName: "상의",
        imageUrl: demoImageUrls.graphicTee,
        dropProduct: true,
        dropStartsAt: liveDropStartsAt,
        dropEndsAt: liveDropEndsAt,
        dropPurchaseLimit: 1,
        dropSaleStatus: "LIVE"
      },
      {
        id: 2,
        itemName: "레더 클래식 재킷 드롭",
        price: 129000,
        quantity: 5,
        categoryId: 1,
        categoryName: "아우터",
        imageUrl: demoImageUrls.leatherJacket,
        dropProduct: true,
        dropStartsAt: upcomingDropStartsAt,
        dropEndsAt: upcomingDropEndsAt,
        dropPurchaseLimit: 1,
        dropSaleStatus: "UPCOMING"
      },
      {
        id: 3,
        itemName: "샌드 트렌치 코트",
        price: 149000,
        quantity: 7,
        categoryId: 1,
        categoryName: "아우터",
        imageUrl: demoImageUrls.trenchCoat
      },
      {
        id: 4,
        itemName: "코지 블루 니트",
        price: 69000,
        quantity: 8,
        categoryId: 2,
        categoryName: "상의",
        imageUrl: demoImageUrls.knitSweater
      },
      {
        id: 5,
        itemName: "스트레이트 데님 팬츠",
        price: 59000,
        quantity: 14,
        categoryId: 3,
        categoryName: "하의",
        imageUrl: demoImageUrls.denimJeans
      },
      {
        id: 6,
        itemName: "화이트 데일리 스니커즈",
        price: 79000,
        quantity: 9,
        categoryId: 4,
        categoryName: "신발",
        imageUrl: demoImageUrls.whiteSneakers
      },
      {
        id: 7,
        itemName: "어반 블랙 후디 종료 드롭",
        price: 62000,
        quantity: 4,
        categoryId: 2,
        categoryName: "상의",
        imageUrl: demoImageUrls.blackHoodie,
        dropProduct: true,
        dropStartsAt: endedDropStartsAt,
        dropEndsAt: endedDropEndsAt,
        dropPurchaseLimit: 1,
        dropSaleStatus: "ENDED"
      },
      {
        id: 8,
        itemName: "레드 러너 스니커즈 드롭",
        price: 89000,
        quantity: 6,
        categoryId: 4,
        categoryName: "신발",
        imageUrl: demoImageUrls.runnerSneakers,
        dropProduct: true,
        dropStartsAt: liveDropStartsAt,
        dropEndsAt: liveDropEndsAt,
        dropPurchaseLimit: 1,
        dropSaleStatus: "LIVE"
      },
      {
        id: 9,
        itemName: "오벌 버킷 토트백 드롭",
        price: 119000,
        quantity: 7,
        categoryId: 5,
        categoryName: "가방",
        imageUrl: demoImageUrls.toteBag,
        dropProduct: true,
        dropStartsAt: upcomingDropStartsAt,
        dropEndsAt: upcomingDropEndsAt,
        dropPurchaseLimit: 1,
        dropSaleStatus: "UPCOMING"
      },
      {
        id: 10,
        itemName: "옐로우 볼캡 선착순 드롭",
        price: 39000,
        quantity: 10,
        categoryId: 6,
        categoryName: "모자",
        imageUrl: demoImageUrls.yellowCap,
        dropProduct: true,
        dropStartsAt: liveDropStartsAt,
        dropEndsAt: liveDropEndsAt,
        dropPurchaseLimit: 2,
        dropSaleStatus: "LIVE"
      },
      {
        id: 11,
        itemName: "스틸 클래식 워치 드롭",
        price: 189000,
        quantity: 4,
        categoryId: 7,
        categoryName: "시계",
        imageUrl: demoImageUrls.classicWatch,
        dropProduct: true,
        dropStartsAt: upcomingDropStartsAt,
        dropEndsAt: upcomingDropEndsAt,
        dropPurchaseLimit: 1,
        dropSaleStatus: "UPCOMING"
      },
      {
        id: 12,
        itemName: "키즈 스트리트 셋업",
        price: 59000,
        quantity: 12,
        categoryId: 8,
        categoryName: "키즈",
        imageUrl: demoImageUrls.kidsSet
      },
      {
        id: 13,
        itemName: "썸머 액세서리 세트",
        price: 49000,
        quantity: 15,
        categoryId: 9,
        categoryName: "액세서리",
        imageUrl: demoImageUrls.accessorySet
      },
      ...additionalDropMarketProducts.map((product, index) => ({
        id: 14 + index,
        ...product
      }))
    ],
    carts: {
      "1": [
        {
          itemId: 1,
          quantity: 1
        },
        {
          itemId: 6,
          quantity: 1
        }
      ],
      "2": [],
      "3": []
    },
    orders: [
      {
        id: 1,
        userId: 1,
        orderDate: "2026-03-14T11:20:00.000Z",
        status: "PAID",
        lineItems: [
          {
            itemId: 2,
            itemName: "레더 클래식 재킷",
            price: 129000,
            quantity: 1
          },
          {
            itemId: 5,
            itemName: "스트레이트 데님 팬츠",
            price: 59000,
            quantity: 1
          }
        ]
      },
      {
        id: 2,
        userId: 1,
        orderDate: "2026-03-02T08:45:00.000Z",
        status: "CANCELLED",
        lineItems: [
          {
            itemId: 4,
            itemName: "코지 블루 니트",
            price: 69000,
            quantity: 1
          }
        ]
      }
    ],
    posts: [
      {
        id: 1,
        authorId: 1,
        author: "서연",
        title: "봄 아우터 고를 때 핏 먼저 보는 편이에요",
        content:
          "요즘은 무조건 튀는 것보다 어깨선이 자연스럽고 오래 입을 수 있는 아우터를 더 찾게 돼요. 트렌치 코트는 슬랙스랑도 잘 어울리고 데님에도 가볍게 매치돼서 손이 자주 갑니다.",
        createdDate: "2026-03-18T10:39:00.000Z",
        comments: [
          {
            id: 1,
            userId: 3,
            username: "미라",
            content: "저도 트렌치 코트는 봄마다 꼭 꺼내 입어요. 데님이랑 조합이 정말 예쁘더라고요.",
            createdDate: "2026-03-18T12:05:00.000Z"
          }
        ]
      },
      {
        id: 2,
        authorId: 3,
        author: "미라",
        title: "화이트 스니커즈는 하나 있으면 진짜 든든해요",
        content:
          "포인트 강한 아이템이 아니어도 신발 하나만 깔끔하면 전체 룩이 정돈되어 보여요. 저는 청바지나 스커트 상관없이 자주 신게 되네요.",
        createdDate: "2026-03-17T06:20:00.000Z",
        comments: [
          {
            id: 2,
            userId: 1,
            username: "서연",
            content: "맞아요. 특히 출근할 때도 주말에도 다 잘 어울려서 손이 자주 가요.",
            createdDate: "2026-03-17T08:10:00.000Z"
          }
        ]
      },
      {
        id: 3,
        authorId: 1,
        author: "서연",
        title: "티셔츠는 결국 자주 입게 되는 기본템이 남더라고요",
        content:
          "프린트가 있더라도 소재가 탄탄하고 단독으로 입었을 때 핏이 깔끔한 티셔츠가 제일 오래 남아요. 가벼운 아우터 안에 받쳐 입기에도 좋아서 활용도가 높아요.",
        createdDate: "2026-03-15T14:10:00.000Z",
        comments: []
      }
    ],
    nextIds: {
      user: 4,
      category: 10,
      item: 14 + additionalDropMarketProducts.length,
      order: 3,
      post: 4,
      comment: 3
    }
  };
}
