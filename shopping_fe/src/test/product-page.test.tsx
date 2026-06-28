import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchItem, fetchItems } from "../api/client";
import { ProductPage } from "../pages/ProductPage";

vi.mock("../api/client", () => ({
  fetchItem: vi.fn(),
  fetchItems: vi.fn(),
  isUnauthorizedError: vi.fn(() => false),
  toAppErrorMessage: vi.fn((_error: unknown, fallback?: string) => fallback ?? "오류")
}));

vi.mock("../contexts/CartContext", () => ({
  useCart: () => ({
    addItem: vi.fn(),
    cart: {
      cartItems: [],
      allPrice: 0
    },
    loading: false,
    refreshCart: vi.fn(),
    removeItem: vi.fn(),
    checkout: vi.fn()
  })
}));

describe("ProductPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows only other items from the same category in the related section", async () => {
    vi.mocked(fetchItem).mockResolvedValue({
      id: 101,
      itemName: "현재 아우터 상품",
      price: 189000,
      quantity: 6,
      categoryId: 8,
      categoryName: "아우터",
      imageUrl: "/current.webp"
    });

    vi.mocked(fetchItems).mockResolvedValue([
      {
        id: 101,
        itemName: "현재 아우터 상품",
        price: 189000,
        quantity: 6,
        categoryId: 8,
        categoryName: "아우터",
        imageUrl: "/current.webp"
      },
      {
        id: 102,
        itemName: "같은 카테고리 A",
        price: 149000,
        quantity: 5,
        categoryId: 8,
        categoryName: "아우터",
        imageUrl: "/outer-a.webp"
      },
      {
        id: 103,
        itemName: "같은 카테고리 B",
        price: 159000,
        quantity: 7,
        categoryId: 8,
        categoryName: "아우터",
        imageUrl: "/outer-b.webp"
      },
      {
        id: 104,
        itemName: "같은 카테고리 C",
        price: 169000,
        quantity: 4,
        categoryId: 8,
        categoryName: "아우터",
        imageUrl: "/outer-c.webp"
      },
      {
        id: 201,
        itemName: "다른 카테고리 상품",
        price: 69000,
        quantity: 8,
        categoryId: 6,
        categoryName: "상의",
        imageUrl: "/top.webp"
      }
    ]);

    render(
      <MemoryRouter initialEntries={["/products/101"]}>
        <Routes>
          <Route path="/products/:productId" element={<ProductPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(
      await screen.findByRole("heading", { level: 1, name: "현재 아우터 상품" })
    ).toBeInTheDocument();

    expect(
      await screen.findByRole("heading", { level: 3, name: "같은 카테고리 A" })
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 3, name: "같은 카테고리 B" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 3, name: "같은 카테고리 C" })).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { level: 3, name: "현재 아우터 상품" })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { level: 3, name: "다른 카테고리 상품" })
    ).not.toBeInTheDocument();
  });

  it("renders the long-form product detail sections with category-specific content", async () => {
    vi.mocked(fetchItem).mockResolvedValue({
      id: 301,
      itemName: "리미티드 러닝 스니커즈",
      price: 129000,
      quantity: 3,
      categoryId: 12,
      categoryName: "신발",
      imageUrl: "/shoe-main.webp",
      dropProduct: true,
      dropStartsAt: "2026-06-26T12:00:00",
      dropEndsAt: "2026-06-26T18:00:00",
      dropPurchaseLimit: 1,
      dropSaleStatus: "LIVE",
      detailImages: [
        {
          id: 1,
          displayOrder: 1,
          imageRole: "MOOD",
          imageUrl: "/shoe-fit.webp",
          altText: "리미티드 러닝 스니커즈 착용 컷",
          caption: "착용 컷"
        },
        {
          id: 2,
          displayOrder: 2,
          imageRole: "MOOD",
          imageUrl: "/shoe-style.webp",
          altText: "리미티드 러닝 스니커즈 무드 컷",
          caption: "무드 컷"
        },
        {
          id: 3,
          displayOrder: 3,
          imageRole: "DETAIL",
          imageUrl: "/shoe-detail.webp",
          altText: "리미티드 러닝 스니커즈 소재 디테일",
          caption: "소재 디테일"
        }
      ]
    });

    vi.mocked(fetchItems).mockResolvedValue([
      {
        id: 301,
        itemName: "리미티드 러닝 스니커즈",
        price: 129000,
        quantity: 3,
        categoryId: 12,
        categoryName: "신발",
        imageUrl: "/shoe-main.webp"
      },
      {
        id: 302,
        itemName: "러닝 무드 A",
        price: 99000,
        quantity: 7,
        categoryId: 12,
        categoryName: "신발",
        imageUrl: "/shoe-mood-a.webp"
      },
      {
        id: 303,
        itemName: "러닝 무드 B",
        price: 109000,
        quantity: 5,
        categoryId: 12,
        categoryName: "신발",
        imageUrl: "/shoe-mood-b.webp"
      },
      {
        id: 304,
        itemName: "러닝 무드 C",
        price: 119000,
        quantity: 4,
        categoryId: 12,
        categoryName: "신발",
        imageUrl: "/shoe-mood-c.webp"
      }
    ]);

    render(
      <MemoryRouter initialEntries={["/products/301"]}>
        <Routes>
          <Route path="/products/:productId" element={<ProductPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(
      await screen.findByRole("heading", { level: 1, name: "리미티드 러닝 스니커즈" })
    ).toBeInTheDocument();

    expect(screen.getByRole("heading", { level: 2, name: "착용/무드 컷" })).toBeInTheDocument();
    expect(screen.getByAltText("리미티드 러닝 스니커즈 착용 컷")).toHaveAttribute(
      "src",
      "/shoe-fit.webp"
    );
    expect(screen.getByAltText("리미티드 러닝 스니커즈 무드 컷")).toHaveAttribute(
      "src",
      "/shoe-style.webp"
    );

    expect(screen.getByRole("heading", { level: 2, name: "소재와 디테일" })).toBeInTheDocument();
    expect(screen.getByAltText("리미티드 러닝 스니커즈 소재 디테일")).toHaveAttribute(
      "src",
      "/shoe-detail.webp"
    );
    expect(screen.getByRole("heading", { level: 2, name: "사이즈 가이드" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "발길이" })).toBeInTheDocument();
    expect(screen.getByText("배송 안내")).toBeInTheDocument();
    expect(screen.getByText("교환/반품 안내")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "추천 상품" })).toBeInTheDocument();
  });
});
