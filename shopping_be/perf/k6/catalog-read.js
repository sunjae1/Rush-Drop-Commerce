import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const categoryId = Number(__ENV.CATEGORY_ID || 1);
const itemId = Number(__ENV.ITEM_ID || 0);
const postId = Number(__ENV.POST_ID || 0);

export const options = {
  scenarios: {
    catalog_read: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "30s", target: 10 },
        { duration: "1m", target: 30 },
        { duration: "30s", target: 0 },
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

function fetch(path) {
  const response = http.get(`${baseUrl}${path}`);
  check(response, {
    [`GET ${path} status is 200`]: (res) => res.status === 200,
  });
  return response;
}

export default function () {
  fetch("/actuator/health");
  fetch("/api/categories");
  fetch("/api/items");
  fetch(`/api/items?categoryId=${categoryId}`);
  fetch("/api/posts?sort=desc");

  if (itemId > 0) {
    fetch(`/api/items/${itemId}`);
  }

  if (postId > 0) {
    fetch(`/api/posts/${postId}`);
  }

  sleep(1);
}
