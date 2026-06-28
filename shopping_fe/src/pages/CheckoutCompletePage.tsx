import { Link, useLocation } from "react-router-dom";
import { StatusBanner } from "../components/StatusBanner";
import type { CheckoutCompleteState } from "../lib/checkoutSnapshot";
import { formatCurrency, formatDateTime, resolveImageUrl } from "../lib/format";

function isCheckoutCompleteState(value: unknown): value is CheckoutCompleteState {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const candidate = value as Partial<CheckoutCompleteState>;
  return Boolean(
    candidate.payment &&
      Array.isArray(candidate.items) &&
      candidate.delivery &&
      candidate.paymentMethodLabel
  );
}

function getScenarioLabel(scenario: CheckoutCompleteState["scenario"]) {
  if (scenario === "failure") {
    return "실패 처리 테스트";
  }

  if (scenario === "amount-mismatch") {
    return "금액 불일치 테스트";
  }

  if (scenario === "toss-success") {
    return "Toss 테스트 결제 승인";
  }

  if (scenario === "toss-failure") {
    return "Toss 테스트 결제 실패";
  }

  return "승인 성공 테스트";
}

export function CheckoutCompletePage() {
  const location = useLocation();
  const state = isCheckoutCompleteState(location.state) ? location.state : null;

  if (!state) {
    return (
      <div className="page-stack">
        <div className="empty-state">
          <p className="eyebrow">PAYMENT RESULT</p>
          <h1>확인할 결제 결과가 없습니다.</h1>
          <p>장바구니에서 주문/결제 페이지로 이동해 결제를 진행해 주세요.</p>
        </div>
        <div className="inline-actions">
          <Link to="/cart" className="primary-button link-button">
            장바구니로 이동
          </Link>
          <Link to="/" className="ghost-button link-button">
            홈으로 이동
          </Link>
        </div>
      </div>
    );
  }

  const { payment, items, delivery, paymentMethodLabel, scenario, amount } = state;
  const isApproved = payment.status === "APPROVED";

  return (
    <div className="page-stack checkout-complete-page">
      <div className="section-header section-header-wide">
        <div>
          <p className="eyebrow">PAYMENT RESULT</p>
          <h1>{isApproved ? "결제가 완료되었습니다." : "결제가 실패 처리되었습니다."}</h1>
          <p className="section-description">
            주문 번호 #{payment.orderId}의 결제 결과입니다.
          </p>
        </div>
        <div className="inline-actions">
          <Link to="/account" className="primary-button link-button">
            마이페이지
          </Link>
          <Link to="/" className="ghost-button link-button">
            쇼핑 계속하기
          </Link>
        </div>
      </div>

      <StatusBanner tone={isApproved ? "success" : "error"}>
        {isApproved
          ? `${payment.provider} 결제가 승인되어 주문 상태가 PAID로 변경되었습니다.`
          : payment.failureReason ?? `${payment.provider} 결제가 실패 처리되었습니다.`}
      </StatusBanner>

      <div className="checkout-complete-layout">
        <section className="surface-card checkout-result-card">
          <p className="eyebrow">PAYMENT KEYS</p>
          <dl className="checkout-result-list">
            <div>
              <dt>테스트 시나리오</dt>
              <dd>{getScenarioLabel(scenario)}</dd>
            </div>
            <div>
              <dt>결제 상태</dt>
              <dd>{payment.status}</dd>
            </div>
            <div>
              <dt>주문 번호</dt>
              <dd>#{payment.orderId}</dd>
            </div>
            <div>
              <dt>우리 결제 주문번호</dt>
              <dd>{payment.paymentOrderId}</dd>
            </div>
            <div>
              <dt>PG 결제키</dt>
              <dd>{payment.providerPaymentKey ?? "승인 전/실패 결제는 발급되지 않음"}</dd>
            </div>
            <div>
              <dt>결제수단</dt>
              <dd>{paymentMethodLabel}</dd>
            </div>
            <div>
              <dt>요청 시각</dt>
              <dd>{formatDateTime(payment.requestedAt)}</dd>
            </div>
            <div>
              <dt>승인 시각</dt>
              <dd>{payment.approvedAt ? formatDateTime(payment.approvedAt) : "-"}</dd>
            </div>
            <div>
              <dt>결제 금액</dt>
              <dd>{formatCurrency(payment.amount || amount)}</dd>
            </div>
          </dl>
        </section>

        <section className="surface-card checkout-result-card">
          <p className="eyebrow">ORDER ITEMS</p>
          <div className="checkout-item-list">
            {items.map((cartItem) => (
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
        </section>

        <section className="surface-card checkout-result-card">
          <p className="eyebrow">DELIVERY</p>
          <dl className="checkout-result-list">
            <div>
              <dt>받는 사람</dt>
              <dd>{delivery.receiver}</dd>
            </div>
            <div>
              <dt>연락처</dt>
              <dd>{delivery.phone}</dd>
            </div>
            <div>
              <dt>주소</dt>
              <dd>{delivery.address}</dd>
            </div>
            <div>
              <dt>배송 메모</dt>
              <dd>{delivery.memo || "-"}</dd>
            </div>
          </dl>
        </section>
      </div>
    </div>
  );
}
