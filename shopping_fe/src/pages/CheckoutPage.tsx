import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  confirmMockPayment,
  failMockPayment,
  failTossPayment,
  isUnauthorizedError,
  prepareMockPayment,
  prepareTossPayment,
  toAppErrorMessage
} from "../api/client";
import type { Payment } from "../api/types";
import { StatusBanner } from "../components/StatusBanner";
import { useCart } from "../contexts/CartContext";
import { useSession } from "../contexts/SessionContext";
import {
  cloneCartItems,
  saveCheckoutSnapshot,
  sumCartItems,
  type CheckoutScenario,
  type DeliverySnapshot
} from "../lib/checkoutSnapshot";
import { formatCurrency, resolveImageUrl } from "../lib/format";

interface Feedback {
  tone: "info" | "success" | "error";
  message: string;
}

const paymentMethodLabels = {
  "toss-card": "Toss 테스트 카드",
  "mock-card": "Mock 카드",
  "mock-transfer": "Mock 계좌이체",
  "mock-wallet": "Mock 간편결제"
};
const TOSS_CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY?.trim() ?? "";
const IS_DEMO_MODE = import.meta.env.VITE_USE_DEMO_DATA === "true";

function buildOrderName(items: ReturnType<typeof cloneCartItems>) {
  if (items.length === 0) {
    return "Seoul Drop Market 주문";
  }

  const firstItemName = items[0].item.itemName;

  if (items.length === 1) {
    return firstItemName;
  }

  return `${firstItemName} 외 ${items.length - 1}건`;
}

function normalizeMobilePhone(value: string) {
  const digits = value.replace(/\D/g, "");
  return digits.length >= 10 ? digits : undefined;
}

function createTossCustomerKey(userId?: number) {
  const randomKey =
    typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : String(Date.now());
  return `customer-${userId ?? "guest"}-${randomKey}`;
}

export function CheckoutPage() {
  const navigate = useNavigate();
  const { user } = useSession();
  const { cart, loading, refreshCart } = useCart();
  const [delivery, setDelivery] = useState<DeliverySnapshot>({
    receiver: user?.name ?? "",
    phone: "010-0000-0000",
    address: "서울시 성동구 드롭로 1",
    memo: "문 앞에 놓아주세요."
  });
  const [paymentMethod, setPaymentMethod] =
    useState<keyof typeof paymentMethodLabels>(
      TOSS_CLIENT_KEY && !IS_DEMO_MODE ? "toss-card" : "mock-card"
    );
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [processingScenario, setProcessingScenario] =
    useState<CheckoutScenario | null>(null);

  const itemCount = cart.cartItems.reduce(
    (sum, cartItem) => sum + cartItem.quantity,
    0
  );
  const deliveryFee = 0;
  const totalAmount = cart.allPrice + deliveryFee;
  const isProcessing = processingScenario !== null;
  const canPay = cart.cartItems.length > 0 && !loading && !isProcessing;
  const paymentMethodOptions = useMemo(
    () => Object.entries(paymentMethodLabels),
    []
  );

  function updateDeliveryField(field: keyof DeliverySnapshot, value: string) {
    setDelivery((currentValue) => ({
      ...currentValue,
      [field]: value
    }));
  }

  async function runPrimaryPayment() {
    if (paymentMethod === "toss-card") {
      await runTossPayment();
      return;
    }

    await runMockPayment("success");
  }

  async function runTossPayment() {
    if (!canPay) {
      setFeedback({
        tone: "error",
        message: "결제할 장바구니 상품이 없습니다."
      });
      return;
    }

    if (IS_DEMO_MODE) {
      setFeedback({
        tone: "error",
        message: "데모 데이터 모드에서는 실제 Toss 결제창을 열 수 없습니다."
      });
      return;
    }

    if (!TOSS_CLIENT_KEY) {
      setFeedback({
        tone: "error",
        message: "프론트엔드 환경변수 VITE_TOSS_CLIENT_KEY를 먼저 설정해주세요."
      });
      return;
    }

    const cartSnapshot = cloneCartItems(cart.cartItems);
    const deliverySnapshot = { ...delivery };
    const amountSnapshot = sumCartItems(cartSnapshot) + deliveryFee;
    setProcessingScenario("toss-success");
    setFeedback({
      tone: "info",
      message: "Toss 결제 준비를 시작했습니다."
    });

    let preparedPayment: Payment | null = null;

    try {
      preparedPayment = await prepareTossPayment();
      saveCheckoutSnapshot(preparedPayment.paymentOrderId, {
        items: cartSnapshot,
        delivery: deliverySnapshot,
        paymentMethodLabel: paymentMethodLabels[paymentMethod],
        scenario: "toss-success",
        amount: amountSnapshot
      });

      const tossPayments = await loadTossPayments(TOSS_CLIENT_KEY);
      const tossPayment = tossPayments.payment({
        customerKey: createTossCustomerKey(user?.id)
      });

      await tossPayment.requestPayment({
        method: "CARD",
        amount: {
          currency: "KRW",
          value: preparedPayment.amount
        },
        orderId: preparedPayment.paymentOrderId,
        orderName: buildOrderName(cartSnapshot),
        successUrl: `${window.location.origin}/checkout/toss/success`,
        failUrl: `${window.location.origin}/checkout/toss/fail`,
        customerEmail: user?.email,
        customerName: delivery.receiver || user?.name || "고객",
        customerMobilePhone: normalizeMobilePhone(delivery.phone),
        card: {
          useEscrow: false,
          flowMode: "DEFAULT",
          useCardPoint: false,
          useAppCardOnly: false
        }
      });
    } catch (error) {
      if (isUnauthorizedError(error)) {
        return;
      }

      if (preparedPayment) {
        await failTossPayment(
          preparedPayment.paymentOrderId,
          toAppErrorMessage(error, "Toss 결제창 호출에 실패했습니다.")
        ).catch(() => undefined);
      }

      setFeedback({
        tone: "error",
        message: toAppErrorMessage(error, "Toss 결제창 호출에 실패했습니다.")
      });
    } finally {
      setProcessingScenario(null);
    }
  }

  async function runMockPayment(scenario: CheckoutScenario) {
    if (!canPay) {
      setFeedback({
        tone: "error",
        message: "결제할 장바구니 상품이 없습니다."
      });
      return;
    }

    const cartSnapshot = cloneCartItems(cart.cartItems);
    const deliverySnapshot = { ...delivery };
    const amountSnapshot = sumCartItems(cartSnapshot) + deliveryFee;
    setProcessingScenario(scenario);
    setFeedback({
      tone: "info",
      message: "Mock 결제 준비를 시작했습니다."
    });

    try {
      const preparedPayment = await prepareMockPayment();
      let resultPayment: Payment;

      if (scenario === "success") {
        resultPayment = await confirmMockPayment(
          preparedPayment.paymentOrderId,
          preparedPayment.amount
        );
      } else if (scenario === "failure") {
        resultPayment = await failMockPayment(
          preparedPayment.paymentOrderId,
          "Mock 카드사 승인 거절 테스트입니다."
        );
      } else {
        try {
          resultPayment = await confirmMockPayment(
            preparedPayment.paymentOrderId,
            preparedPayment.amount + 1000
          );
        } catch (error) {
          if (isUnauthorizedError(error)) {
            return;
          }

          resultPayment = {
            ...preparedPayment,
            status: "FAILED",
            failureReason: toAppErrorMessage(error)
          };
        }
      }

      await refreshCart().catch(() => undefined);

      navigate("/checkout/complete", {
        state: {
          payment: resultPayment,
          items: cartSnapshot,
          delivery: deliverySnapshot,
          paymentMethodLabel: paymentMethodLabels[paymentMethod],
          scenario,
          amount: amountSnapshot
        }
      });
    } catch (error) {
      if (isUnauthorizedError(error)) {
        return;
      }

      setFeedback({
        tone: "error",
        message: toAppErrorMessage(error)
      });
    } finally {
      setProcessingScenario(null);
    }
  }

  return (
    <div className="page-stack checkout-page">
      <div className="section-header section-header-wide">
        <div>
          <p className="eyebrow">CHECKOUT</p>
          <h1>주문/결제</h1>
            <p className="section-description">
            주문 금액, 배송지, 결제수단을 확인한 뒤 Toss 테스트 결제 또는 Mock 시나리오를 실행합니다.
          </p>
        </div>
        <Link to="/cart" className="ghost-button link-button">
          장바구니로 돌아가기
        </Link>
      </div>

      <StatusBanner tone={feedback?.tone}>{feedback?.message}</StatusBanner>

      <div className="checkout-layout">
        <section className="surface-card checkout-order-panel">
          <div className="checkout-panel-head">
            <div>
              <p className="eyebrow">ORDER ITEMS</p>
              <h2>주문 상품</h2>
            </div>
            <span className="checkout-count">{itemCount}개</span>
          </div>

          {loading ? (
            <p className="muted-copy">장바구니를 불러오는 중입니다.</p>
          ) : cart.cartItems.length > 0 ? (
            <div className="checkout-item-list">
              {cart.cartItems.map((cartItem) => (
                <article key={cartItem.item.id} className="checkout-item-row">
                  <img
                    src={resolveImageUrl(cartItem.item.imageUrl)}
                    alt={cartItem.item.itemName}
                  />
                  <div>
                    <h3>{cartItem.item.itemName}</h3>
                    <p>{formatCurrency(cartItem.item.price)}</p>
                    <span>수량 {cartItem.quantity}</span>
                  </div>
                  <strong>
                    {formatCurrency(cartItem.item.price * cartItem.quantity)}
                  </strong>
                </article>
              ))}
            </div>
          ) : (
            <div className="checkout-empty">
              <strong>결제할 상품이 없습니다.</strong>
              <p>드롭 상품을 장바구니에 담은 뒤 결제를 진행할 수 있습니다.</p>
            </div>
          )}
        </section>

        <section className="surface-card checkout-form-panel">
          <div className="checkout-panel-head">
            <div>
              <p className="eyebrow">DELIVERY</p>
              <h2>배송지</h2>
            </div>
          </div>

          <div className="checkout-form-grid">
            <label className="auth-field">
              받는 사람
              <input
                value={delivery.receiver}
                onChange={(event) =>
                  updateDeliveryField("receiver", event.target.value)
                }
              />
            </label>
            <label className="auth-field">
              연락처
              <input
                value={delivery.phone}
                onChange={(event) =>
                  updateDeliveryField("phone", event.target.value)
                }
              />
            </label>
            <label className="auth-field checkout-form-wide">
              주소
              <input
                value={delivery.address}
                onChange={(event) =>
                  updateDeliveryField("address", event.target.value)
                }
              />
            </label>
            <label className="auth-field checkout-form-wide">
              배송 메모
              <input
                value={delivery.memo}
                onChange={(event) =>
                  updateDeliveryField("memo", event.target.value)
                }
              />
            </label>
          </div>
        </section>

        <aside className="checkout-sidebar">
          <section className="surface-card checkout-payment-panel">
            <p className="eyebrow">PAYMENT METHOD</p>
            <h2>결제수단</h2>
            <div className="checkout-method-list" role="radiogroup" aria-label="결제수단">
              {paymentMethodOptions.map(([value, label]) => (
                <label
                  key={value}
                  className={`checkout-method ${
                    paymentMethod === value ? "checkout-method-active" : ""
                  }`}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value={value}
                    checked={paymentMethod === value}
                    onChange={() =>
                      setPaymentMethod(value as keyof typeof paymentMethodLabels)
                    }
                  />
                  <span>{label}</span>
                </label>
              ))}
            </div>
          </section>

          <section className="surface-card checkout-summary-panel">
            <p className="eyebrow">PAYMENT SUMMARY</p>
            <dl className="cart-calculation-list">
              <div className="cart-calculation-row">
                <dt>상품 합계</dt>
                <dd>{formatCurrency(cart.allPrice)}</dd>
              </div>
              <div className="cart-calculation-row">
                <dt>배송비</dt>
                <dd>{formatCurrency(deliveryFee)}</dd>
              </div>
              <div className="cart-calculation-row cart-calculation-total">
                <dt>총 결제</dt>
                <dd>{formatCurrency(totalAmount)}</dd>
              </div>
            </dl>

            <button
              type="button"
              className="primary-button checkout-pay-button"
              disabled={!canPay}
              onClick={() => void runPrimaryPayment()}
            >
              {processingScenario === "toss-success"
                ? "Toss 결제창 준비 중..."
                : paymentMethod === "toss-card"
                  ? "Toss 결제창 열기"
                  : processingScenario === "success"
                    ? "승인 처리 중..."
                    : "Mock 카드 결제"}
            </button>
          </section>

          <section className="surface-card checkout-test-panel">
            <p className="eyebrow">MOCK TEST</p>
            <h2>승인/실패 테스트</h2>
            <div className="checkout-test-actions">
              <button
                type="button"
                className="secondary-button"
                disabled={!canPay}
                onClick={() => void runMockPayment("success")}
              >
                승인 성공 테스트
              </button>
              <button
                type="button"
                className="ghost-button"
                disabled={!canPay}
                onClick={() => void runMockPayment("failure")}
              >
                실패 처리 테스트
              </button>
              <button
                type="button"
                className="ghost-button"
                disabled={!canPay}
                onClick={() => void runMockPayment("amount-mismatch")}
              >
                금액 불일치 테스트
              </button>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}
