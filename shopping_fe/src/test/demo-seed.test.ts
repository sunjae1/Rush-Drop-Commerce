import { describe, expect, it } from "vitest";
import { createDemoSeed } from "../api/demoSeed";

describe("createDemoSeed", () => {
  it("includes expanded drop-market categories with representative product images", () => {
    const seed = createDemoSeed();
    const categoryNames = seed.categories.map((category) => category.name);
    const requiredCategories = ["신발", "가방", "모자", "시계", "키즈", "액세서리"];

    expect(categoryNames).toEqual(expect.arrayContaining(requiredCategories));

    for (const categoryName of requiredCategories) {
      const category = seed.categories.find((entry) => entry.name === categoryName);
      const categoryItems = seed.items.filter((item) => item.categoryId === category?.id);
      const representativeItem = seed.items.find(
        (item) => item.categoryId === category?.id && item.imageUrl?.startsWith("https://images.pexels.com/")
      );

      expect(categoryItems.length, `${categoryName} 카테고리 상품 수`).toBeGreaterThanOrEqual(10);
      expect(representativeItem, `${categoryName} 카테고리 대표 상품`).toBeDefined();
    }
  });
});
