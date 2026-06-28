import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createItem,
  deleteItem,
  fetchCategories,
  fetchItems,
  updateItem
} from "../api/client";
import { AdminItemsPage } from "../pages/AdminItemsPage";

vi.mock("../api/client", () => ({
  createItem: vi.fn(),
  deleteItem: vi.fn(),
  fetchCategories: vi.fn(),
  fetchItems: vi.fn(),
  toAppErrorMessage: vi.fn((_error: unknown, fallback: string) => fallback),
  updateItem: vi.fn()
}));

describe("AdminItemsPage", () => {
  beforeEach(() => {
    vi.mocked(fetchCategories).mockResolvedValue([
      {
        id: 1,
        name: "Outer"
      }
    ]);
    vi.mocked(fetchItems)
      .mockResolvedValueOnce([
        {
          id: 11,
          itemName: "Active Coat",
          price: 189000,
          quantity: 5,
          categoryId: 1,
          categoryName: "Outer",
          imageUrl: "/active.webp"
        }
      ])
      .mockResolvedValueOnce([
        {
          id: 12,
          itemName: "Deleted Hoodie",
          price: 62000,
          quantity: 0,
          deleted: true,
          categoryId: 1,
          categoryName: "Outer",
          imageUrl: "/deleted.webp"
        }
      ]);
    vi.mocked(fetchItems).mockResolvedValue([
      {
        id: 12,
        itemName: "Deleted Hoodie",
        price: 62000,
        quantity: 0,
        deleted: true,
        categoryId: 1,
        categoryName: "Outer",
        imageUrl: "/deleted.webp"
      }
    ]);
    vi.mocked(createItem).mockResolvedValue({
      id: 13,
      itemName: "New Item",
      price: 10000,
      quantity: 1,
      categoryId: 1,
      categoryName: "Outer",
      imageUrl: "/new.webp"
    });
    vi.mocked(updateItem).mockResolvedValue({
      id: 11,
      itemName: "Active Coat",
      price: 189000,
      quantity: 5,
      categoryId: 1,
      categoryName: "Outer",
      imageUrl: "/active.webp"
    });
    vi.mocked(deleteItem).mockResolvedValue();
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("loads deleted items only when the admin switches the visibility filter", async () => {
    render(
      <MemoryRouter>
        <AdminItemsPage />
      </MemoryRouter>
    );

    expect(
      await screen.findByRole("heading", { level: 3, name: "Active Coat" })
    ).toBeInTheDocument();
    expect(vi.mocked(fetchItems)).toHaveBeenLastCalledWith({
      keyword: "",
      categoryId: null,
      deleted: false
    });

    fireEvent.change(screen.getByRole("combobox", { name: "노출 상태" }), {
      target: {
        value: "deleted"
      }
    });

    expect(
      await screen.findByRole("heading", { level: 3, name: "Deleted Hoodie" })
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(vi.mocked(fetchItems)).toHaveBeenLastCalledWith({
        keyword: "",
        categoryId: null,
        deleted: true
      });
    });

    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
    expect(screen.getByText("이미 삭제된 상품입니다.")).toBeInTheDocument();
  });
});
