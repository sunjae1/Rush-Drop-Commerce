export type UserRole = "USER" | "ADMIN";

export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
}

export interface Item {
  id: number;
  itemName: string;
  price: number;
  quantity: number;
  deleted?: boolean;
  categoryId?: number | null;
  categoryName?: string | null;
  imageUrl?: string | null;
  dropProduct?: boolean;
  dropStartsAt?: string | null;
  dropEndsAt?: string | null;
  dropPurchaseLimit?: number | null;
  dropSaleStatus?: DropSaleStatus;
  detailImages?: ItemDetailImage[];
}

export type DropSaleStatus = "STANDARD" | "UPCOMING" | "LIVE" | "ENDED";
export type ItemDetailImageRole = "MOOD" | "DETAIL";

export interface ItemDetailImage {
  id: number;
  displayOrder: number;
  imageRole: ItemDetailImageRole;
  imageUrl: string;
  altText: string;
  caption?: string | null;
}

export interface Category {
  id: number;
  name: string;
  representativeImageUrl?: string | null;
  itemCount?: number;
}

export interface ItemMutationInput {
  itemName: string;
  price: number;
  quantity: number;
  categoryId: number;
  imageFile?: File | null;
  dropProduct?: boolean;
  dropStartsAt?: string | null;
  dropEndsAt?: string | null;
  dropPurchaseLimit?: number | null;
}

export interface CartItem {
  item: Item;
  quantity: number;
}

export interface Cart {
  cartItems: CartItem[];
  allPrice: number;
}

export interface OrderItem {
  itemName: string;
  price: number;
  quantity: number;
}

export interface Order {
  id: number;
  orderItems: OrderItem[];
  orderDate: string;
  status: string;
}

export interface Payment {
  id: number;
  orderId: number;
  paymentOrderId: string;
  provider: string;
  status: string;
  amount: number;
  providerPaymentKey?: string | null;
  requestedAt: string;
  approvedAt?: string | null;
  failureReason?: string | null;
}

export interface Comment {
  id: number;
  content: string;
  username: string;
  createdDate: string;
}

export type PostSortOrder = "asc" | "desc";

export interface Post {
  id: number;
  title: string;
  content: string;
  author: string;
  comments: Comment[];
  createdDate: string;
}

export interface MyPage {
  user: User;
  orders: Order[];
  posts: Post[];
  cartItems: Item[];
}

export interface RawUserResponse {
  userDto: User;
  itemDto: Item[];
}

export interface LoginResponse {
  accessTokenExpiresInSeconds: number;
  user: User;
}

export interface SessionPayload {
  user: User | null;
  items: Item[];
}

export interface RawPost {
  id: number;
  title: string;
  content: string;
  author?: string;
  authorName?: string;
  comments: Comment[];
  createdDate: string;
}
