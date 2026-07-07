package team.startup.gwangsan.global.thirdparty.expo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpoPushAdapter implements NotificationPort {

    private final RetryTemplate retryTemplate;

    private final WebClient expoClient = WebClient.builder()
            .baseUrl("https://exp.host")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    @Override
    public void sendNotification(List<DeviceToken> deviceTokens, String title, String body, NotificationType type, Long sourceId) {
        if (deviceTokens == null || deviceTokens.isEmpty()) return;

        List<String> expoTokens = deviceTokens.stream()
                .map(DeviceToken::getDeviceToken)
                .filter(Objects::nonNull)
                .filter(this::isExpoPushToken)
                .toList();

        if (!expoTokens.isEmpty()) {
            sendByExpo(expoTokens, title, body, type, sourceId);
        }
    }

    boolean isExpoPushToken(String t) {
        return t.startsWith("ExponentPushToken[");
    }

    Map<String, String> buildData(NotificationType type, Long sourceId) {
        Map<String, String> data = new HashMap<>();
        data.put("alertType", type.name());
        data.put("sourceId", String.valueOf(sourceId));
        if (type == NotificationType.CHATTING) {
            data.put("roomId", String.valueOf(sourceId));
        }
        return data;
    }

    private void sendByExpo(List<String> expoTokens,
                             String title,
                             String body,
                             NotificationType type,
                             Long sourceId) {

        final int LIMIT = 100;
        for (int i = 0; i < expoTokens.size(); i += LIMIT) {
            List<String> chunk = expoTokens.subList(i, Math.min(i + LIMIT, expoTokens.size()));

            List<Map<String, Object>> payload = chunk.stream()
                    .map(t -> Map.<String, Object>of(
                            "to", t,
                            "title", title,
                            "body", body,
                            "sound", "default",
                            "data", buildData(type, sourceId)
                    ))
                    .toList();

            try {
                retryTemplate.execute(ctx -> {
                    var resp = expoClient.post()
                            .uri("/--/api/v2/push/send")
                            .bodyValue(payload)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

                    log.info("[Expo] 응답: {}", resp);
                    return null;
                });
            } catch (Exception e) {
                log.error("[Expo] 전송 실패: {}", e.getMessage(), e);
            }
        }
    }
}