import type { DropSaleStatus, Item } from "../api/types";
import { formatDateTime } from "./format";

export function getDropSaleStatus(item: Item): DropSaleStatus {
  return item.dropSaleStatus ?? "STANDARD";
}

export function getDropSaleLabel(item: Item): string | null {
  if (!item.dropProduct) {
    return null;
  }

  switch (getDropSaleStatus(item)) {
    case "UPCOMING":
      return "드롭 예정";
    case "LIVE":
      return "드롭 진행";
    case "ENDED":
      return "드롭 종료";
    default:
      return "드롭 상품";
  }
}

export function getDropSaleTone(item: Item): "live" | "limited" | "soldout" | "upcoming" {
  switch (getDropSaleStatus(item)) {
    case "LIVE":
      return "live";
    case "UPCOMING":
      return "upcoming";
    case "ENDED":
      return "soldout";
    default:
      return "limited";
  }
}

export function getDropSaleScheduleText(item: Item): string | null {
  if (!item.dropProduct) {
    return null;
  }

  if (item.dropStartsAt && item.dropEndsAt) {
    return `${formatDateTime(item.dropStartsAt)} - ${formatDateTime(item.dropEndsAt)}`;
  }

  if (item.dropStartsAt) {
    return `${formatDateTime(item.dropStartsAt)} 오픈`;
  }

  if (item.dropEndsAt) {
    return `${formatDateTime(item.dropEndsAt)} 종료`;
  }

  return null;
}
