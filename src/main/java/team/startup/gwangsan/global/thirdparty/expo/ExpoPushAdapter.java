package team.startup.gwangsan.global.thirdparty.expo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ExpoPushAdapter implements NotificationPort {

    private final RetryTemplate retryTemplate;
    private final WebClient expoClient;
    private final Gson gson;

    public ExpoPushAdapter(RetryTemplate retryTemplate, WebClient.Builder webClientBuilder, Gson gson) {
        this.retryTemplate = retryTemplate;
        this.gson = gson;
        this.expoClient = webClientBuilder
                .baseUrl("https://exp.host")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void sendNotification(List<DeviceToken> deviceTokens, String title, String body, NotificationType type, Long sourceId) {
        if (deviceTokens == null || deviceTokens.isEmpty()) return;
        if (type == null) {
            log.warn("[Expo] NotificationType이 null이므로 푸시 알림 전송을 중단합니다.");
            return;
        }

        List<String> expoTokens = expoTokens(deviceTokens);

        if (!expoTokens.isEmpty()) {
            sendByExpo(expoTokens, title, body, type, sourceId);
        }
    }

    boolean isExpoPushToken(String t) {
        return t != null && t.startsWith("ExponentPushToken[");
    }

    Map<String, String> buildData(NotificationType type, Long sourceId) {
        Map<String, String> data = new HashMap<>();
        data.put("alertType", type.name());
        if (sourceId != null) {
            data.put("sourceId", String.valueOf(sourceId));
            if (type == NotificationType.CHATTING) {
                data.put("roomId", String.valueOf(sourceId));
            }
        }
        return data;
    }

    private void sendByExpo(List<String> expoTokens,
                             String title,
                             String body,
                             NotificationType type,
                             Long sourceId) {

        final int LIMIT = 100;
        Map<String, String> data = buildData(type, sourceId);

        for (int i = 0; i < expoTokens.size(); i += LIMIT) {
            List<String> chunk = expoTokens.subList(i, Math.min(i + LIMIT, expoTokens.size()));

            List<Map<String, Object>> payload = chunk.stream()
                    .map(t -> {
                        Map<String, Object> message = new HashMap<>();
                        message.put("to", t);
                        message.put("title", title);
                        message.put("body", body);
                        message.put("sound", "default");
                        message.put("data", data);
                        return message;
                    })
                    .toList();

            try {
                retryTemplate.execute(ctx -> {
                    var resp = expoClient.post()
                            .uri("/--/api/v2/push/send")
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(5));

                    log.info("[Expo] 응답: {}", resp);
                    logExpoErrors(resp);
                    return null;
                });
            } catch (Exception e) {
                log.error("[Expo] 전송 실패: {}", e.getMessage(), e);
            }
        }
    }

    List<String> expoTokens(List<DeviceToken> deviceTokens) {
        return deviceTokens.stream()
                .filter(Objects::nonNull)
                .map(DeviceToken::getDeviceToken)
                .filter(Objects::nonNull)
                .filter(this::isExpoPushToken)
                .distinct()
                .toList();
    }

    int logExpoErrors(String response) {
        if (response == null || response.isBlank()) return 0;
        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            if (root == null || !root.has("data") || !root.get("data").isJsonArray()) return 0;

            JsonArray data = root.getAsJsonArray("data");
            int errors = 0;
            for (JsonElement element : data) {
                if (!element.isJsonObject()) continue;
                JsonObject ticket = element.getAsJsonObject();
                if (!"error".equals(asString(ticket, "status"))) continue;
                errors++;

                JsonObject details = ticket.has("details") && ticket.get("details").isJsonObject()
                        ? ticket.getAsJsonObject("details")
                        : null;
                log.warn(
                        "[Expo] ticket error: message={}, error={}, details={}",
                        asString(ticket, "message"),
                        details == null ? null : asString(details, "error"),
                        details
                );
            }
            return errors;
        } catch (Exception e) {
            log.warn("[Expo] 응답 파싱 실패: {}", e.getMessage());
            return 0;
        }
    }

    private String asString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }
}
