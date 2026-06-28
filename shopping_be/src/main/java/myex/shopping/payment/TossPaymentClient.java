package myex.shopping.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import myex.shopping.exception.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TossPaymentClient implements TossPaymentGateway {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.toss.secret-key:}")
    private String secretKey;

    @Value("${app.payment.toss.base-url:https://api.tosspayments.com}")
    private String baseUrl;

    @Override
    public TossPaymentConfirmation confirm(String paymentKey, String orderId, int amount) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new PaymentException(
                    "TOSS_SECRET_KEY_REQUIRED",
                    "토스 시크릿 키가 설정되지 않았습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            TossConfirmResponse response = restClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .uri("/v1/payments/confirm")
                    .header("Authorization", buildAuthorizationHeader(secretKey))
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount))
                    .retrieve()
                    .body(TossConfirmResponse.class);

            if (response == null || response.paymentKey() == null || response.orderId() == null) {
                throw new PaymentException(
                        "TOSS_CONFIRM_EMPTY_RESPONSE",
                        "토스 결제 승인 응답이 올바르지 않습니다.",
                        HttpStatus.BAD_GATEWAY);
            }

            return new TossPaymentConfirmation(
                    response.paymentKey(),
                    response.orderId(),
                    response.status(),
                    response.totalAmount());
        } catch (RestClientResponseException ex) {
            TossErrorResponse error = parseTossError(ex);
            throw new PaymentException(
                    error.code(),
                    error.message(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private String buildAuthorizationHeader(String rawSecretKey) {
        String token = Base64.getEncoder()
                .encodeToString((rawSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private TossErrorResponse parseTossError(RestClientResponseException ex) {
        try {
            JsonNode json = objectMapper.readTree(ex.getResponseBodyAsString());
            String code = json.path("code").asText("TOSS_CONFIRM_FAILED");
            String message = json.path("message").asText("토스 결제 승인에 실패했습니다.");
            return new TossErrorResponse(code, message);
        } catch (Exception ignored) {
            return new TossErrorResponse("TOSS_CONFIRM_FAILED", "토스 결제 승인에 실패했습니다.");
        }
    }

    private record TossConfirmResponse(
            String paymentKey,
            String orderId,
            String status,
            int totalAmount
    ) {
    }

    private record TossErrorResponse(
            String code,
            String message
    ) {
    }
}
