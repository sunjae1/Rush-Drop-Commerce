import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  failTossPayment,
  isUnauthorizedError,
  toAppErrorMessage
} from "../api/client";
import { StatusBanner } from "../components/StatusBanner";
import {
  loadCheckoutSnapshot,
  removeCheckoutSnapshot,
  type CheckoutCompleteState
} from "../lib/checkoutSnapshot";

export function TossPaymentFailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const code = searchParams.get("code");
    const message = searchParams.get("message");
    const orderId = searchParams.get("orderId");
    const reason = [code, message].filter(Boolean).join(" : ") || "Toss 결제창 실패";

    if (!orderId) {
      setErrorMessage(reason);
      return;
    }

    const snapshot = loadCheckoutSnapshot(orderId);

    void failTossPayment(orderId, reason)
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
          scenario: "toss-failure",
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

        setErrorMessage(toAppErrorMessage(error, reason));
      });
  }, [navigate, searchParams]);

  return (
    <div className="page-stack">
      <div className="surface-card">
        <p className="eyebrow">TOSS FAILED</p>
        <h1>토스 결제가 완료되지 않았습니다.</h1>
        <p className="section-description">
          결제창에서 돌아온 실패 정보를 정리하고 예약 재고를 복구하고 있습니다.
        </p>
      </div>

      <StatusBanner tone="error">{errorMessage}</StatusBanner>

      {errorMessage ? (
        <div className="inline-actions">
          <Link to="/checkout" className="primary-button link-button">
            결제 다시 시도
          </Link>
          <Link to="/cart" className="ghost-button link-button">
            장바구니
          </Link>
        </div>
      ) : null}
    </div>
  );
}
