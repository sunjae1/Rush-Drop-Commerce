import type { CartItem, Payment } from "../api/types";

export type CheckoutScenario =
  | "success"
  | "failure"
  | "amount-mismatch"
  | "toss-success"
  | "toss-failure";

export interface DeliverySnapshot {
  receiver: string;
  phone: string;
  address: string;
  memo: string;
}

export interface CheckoutSnapshot {
  items: CartItem[];
  delivery: DeliverySnapshot;
  paymentMethodLabel: string;
  scenario: CheckoutScenario;
  amount: number;
}

export interface CheckoutCompleteState extends CheckoutSnapshot {
  payment: Payment;
}

const CHECKOUT_SNAPSHOT_PREFIX = "shopping.checkout.";

function getStorage(): Storage | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

export function cloneCartItems(items: CartItem[]): CartItem[] {
  return items.map((cartItem) => ({
    quantity: cartItem.quantity,
    item: {
      ...cartItem.item,
      detailImages: cartItem.item.detailImages
        ? cartItem.item.detailImages.map((detailImage) => ({ ...detailImage }))
        : undefined
    }
  }));
}

export function sumCartItems(items: CartItem[]) {
  return items.reduce(
    (sum, cartItem) => sum + cartItem.item.price * cartItem.quantity,
    0
  );
}

export function saveCheckoutSnapshot(orderId: string, snapshot: CheckoutSnapshot) {
  getStorage()?.setItem(
    CHECKOUT_SNAPSHOT_PREFIX + orderId,
    JSON.stringify(snapshot)
  );
}

export function loadCheckoutSnapshot(orderId: string | null): CheckoutSnapshot | null {
  if (!orderId) {
    return null;
  }

  const raw = getStorage()?.getItem(CHECKOUT_SNAPSHOT_PREFIX + orderId);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as CheckoutSnapshot;
  } catch {
    return null;
  }
}

export function removeCheckoutSnapshot(orderId: string | null) {
  if (!orderId) {
    return;
  }

  getStorage()?.removeItem(CHECKOUT_SNAPSHOT_PREFIX + orderId);
}
