import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  confirmTossPayment,
  isUnauthorizedError,
  toAppErrorMessage
} from "../api/client";
import { StatusBanner } from "../components/StatusBanner";
import {
  loadCheckoutSnapshot,
  removeCheckoutSnapshot,
  type CheckoutCompleteState
} from "../lib/checkoutSnapshot";

export function TossPaymentSuccessPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const paymentKey = searchParams.get("paymentKey");
    const orderId = searchParams.get("orderId");
    const amountText = searchParams.get("amount");
    const amount = amountText ? Number(amountText) : NaN;

    if (!paymentKey || !orderId || !Number.isFinite(amount)) {
      setErrorMessage("토스 결제 승인 파라미터가 올바르지 않습니다.");
      return;
    }

    const snapshot = loadCheckoutSnapshot(orderId);

    void confirmTossPayment({
      paymentKey,
      orderId,
      amount
    })
      .then((payment) => {
        removeCheckoutSnapshot(orderId);

        const completeState: CheckoutCompleteState = {
          payment,
          items: snapshot?.items ?? [],
          delivery:
            snapshot?.delivery ?? {
              receiver: "",
              phone: "",
              address: "",
              memo: ""
            },
          paymentMethodLabel: snapshot?.paymentMethodLabel ?? "Toss 테스트 카드",
          scenario: "toss-success",
          amount: snapshot?.amount ?? payment.amount
        };

        navigate("/checkout/complete", {
          replace: true,
          state: completeState
        });
      })
      .catch((error) => {
        if (isUnauthorizedError(error)) {
          return;
        }

        setErrorMessage(toAppErrorMessage(error, "토스 결제 승인에 실패했습니다."));
      });
  }, [navigate, searchParams]);

  return (
    <div className="page-stack">
      <div className="surface-card">
        <p className="eyebrow">TOSS CONFIRM</p>
        <h1>토스 결제를 승인하는 중입니다.</h1>
        <p className="section-description">
          결제 인증 결과를 서버에서 검증하고 토스 승인 API를 호출하고 있습니다.
        </p>
      </div>

      <StatusBanner tone="error">{errorMessage}</StatusBanner>

      {errorMessage ? (
        <div className="inline-actions">
          <Link to="/checkout" className="primary-button link-button">
            결제 페이지로 돌아가기
          </Link>
          <Link to="/cart" className="ghost-button link-button">
            장바구니
          </Link>
        </div>
      ) : null}
    </div>
  );
}
