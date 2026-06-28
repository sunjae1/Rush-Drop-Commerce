import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const email = __ENV.USER_EMAIL || "perf_20260323020402_1@example.com";
const password = __ENV.USER_PASSWORD || "pw1234!";
const itemId = Number(__ENV.ITEM_ID || 10);
const quantity = Number(__ENV.QUANTITY || 1);
const enableWrite = (__ENV.ENABLE_WRITE || "false").toLowerCase() === "true";

export const options = {
  scenarios: {
    auth_user_flow: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "30s", target: 5 },
        { duration: "1m", target: 15 },
        { duration: "30s", target: 0 },
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<700"],
  },
};

function checkStatus(response, expectedStatus, label) {
  check(response, {
    [`${label} status is ${expectedStatus}`]: (res) => res.status === expectedStatus,
  });
}

function login() {
  const payload = JSON.stringify({ email, password });
  const response = http.post(`${baseUrl}/api/login`, payload, {
    headers: { "Content-Type": "application/json" },
  });
  checkStatus(response, 200, "POST /api/login");
}

function get(path) {
  const response = http.get(`${baseUrl}${path}`);
  checkStatus(response, 200, `GET ${path}`);
  return response;
}

function postJson(path, body, expectedStatus = 200) {
  const response = http.post(`${baseUrl}${path}`, JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
  });
  checkStatus(response, expectedStatus, `POST ${path}`);
  return response;
}

export default function () {
  login();

  get("/api/cart");
  get("/api/myPage");
  get("/api/orders");
  get("/api");

  if (enableWrite) {
    postJson(`/api/cart/items/${itemId}`, { id: itemId, quantity }, 200);
    postJson("/api/orders", {}, 201);
  }

  sleep(1);
}
