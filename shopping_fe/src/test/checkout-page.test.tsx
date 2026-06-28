import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { CheckoutCompletePage } from "../pages/CheckoutCompletePage";
import { CheckoutPage } from "../pages/CheckoutPage";

const prepareMockPayment = vi.fn();
const confirmMockPayment = vi.fn();
const failMockPayment = vi.fn();
const prepareTossPayment = vi.fn();
const failTossPayment = vi.fn();
const refreshCart = vi.fn();

vi.mock("../api/client", () => ({
  confirmMockPayment: (...args: unknown[]) => confirmMockPayment(...args),
  failMockPayment: (...args: unknown[]) => failMockPayment(...args),
  failTossPayment: (...args: unknown[]) => failTossPayment(...args),
  isUnauthorizedError: vi.fn(() => false),
  prepareMockPayment: (...args: unknown[]) => prepareMockPayment(...args),
  prepareTossPayment: (...args: unknown[]) => prepareTossPayment(...args),
  toAppErrorMessage: vi.fn((error?: unknown) =>
    error instanceof Error ? error.message : "오류"
  )
}));

vi.mock("../contexts/SessionContext", () => ({
  useSession: () => ({
    user: {
      id: 1,
      email: "member@example.com",
      name: "멤버",
      role: "USER"
    }
  })
}));

vi.mock("../contexts/CartContext", () => ({
  useCart: () => ({
    cart: {
      cartItems: [
        {
          item: {
            id: 1,
            itemName: "드롭 스니커즈",
            price: 12000,
            quantity: 5,
            imageUrl: null
          },
          quantity: 1
        }
      ],
      allPrice: 12000
    },
    loading: false,
    refreshCart
  })
}));

function renderCheckout() {
  return render(
    <MemoryRouter initialEntries={["/checkout"]}>
      <Routes>
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/checkout/complete" element={<CheckoutCompletePage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("CheckoutPage", () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    vi.clearAllMocks();
    refreshCart.mockResolvedValue(undefined);
  });

  it("prepares and confirms a mock card payment", async () => {
    prepareMockPayment.mockResolvedValue({
      id: 10,
      orderId: 77,
      paymentOrderId: "mock_order_77",
      provider: "MOCK",
      status: "READY",
      amount: 12000,
      providerPaymentKey: null,
      requestedAt: "2026-06-26T00:00:00",
      approvedAt: null,
      failureReason: null
    });
    confirmMockPayment.mockResolvedValue({
      id: 10,
      orderId: 77,
      paymentOrderId: "mock_order_77",
      provider: "MOCK",
      status: "APPROVED",
      amount: 12000,
      providerPaymentKey: "mock_mock_order_77",
      requestedAt: "2026-06-26T00:00:00",
      approvedAt: "2026-06-26T00:01:00",
      failureReason: null
    });

    renderCheckout();

    fireEvent.click(screen.getByRole("button", { name: "승인 성공 테스트" }));

    expect(await screen.findByText("결제가 완료되었습니다.")).toBeVisible();
    expect(confirmMockPayment).toHaveBeenCalledWith("mock_order_77", 12000);
    expect(screen.getByText("mock_mock_order_77")).toBeVisible();
  });

  it("shows amount mismatch as a failed payment result", async () => {
    prepareMockPayment.mockResolvedValue({
      id: 11,
      orderId: 78,
      paymentOrderId: "mock_order_78",
      provider: "MOCK",
      status: "READY",
      amount: 12000,
      providerPaymentKey: null,
      requestedAt: "2026-06-26T00:00:00",
      approvedAt: null,
      failureReason: null
    });
    confirmMockPayment.mockRejectedValue(
      new Error("요청 금액과 결제 금액이 일치하지 않습니다.")
    );

    renderCheckout();

    fireEvent.click(screen.getByRole("button", { name: "금액 불일치 테스트" }));

    expect(await screen.findByText("결제가 실패 처리되었습니다.")).toBeVisible();
    expect(confirmMockPayment).toHaveBeenCalledWith("mock_order_78", 13000);
    expect(screen.getByText("금액 불일치 테스트")).toBeVisible();
  });
});
