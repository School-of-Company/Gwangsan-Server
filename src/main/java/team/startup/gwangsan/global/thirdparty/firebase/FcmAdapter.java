package team.startup.gwangsan.global.thirdparty.firebase;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import team.startup.gwangsan.domain.auth.entity.constant.OsType;
import team.startup.gwangsan.domain.notification.NotificationPort;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmAdapter implements NotificationPort {

    private final RetryTemplate retryTemplate;

    @Override
    public void sendNotification(List<DeviceToken> deviceTokens, String title, String body, NotificationType type, Long sourceId) {
        if (deviceTokens == null || deviceTokens.isEmpty()) return;

        List<String> androidTokens = deviceTokens.stream()
                .filter(dt -> dt.getOsType() == OsType.ANDROID)
                .map(DeviceToken::getDeviceToken)
                .filter(Objects::nonNull)
                .toList();

        List<String> iosExpoTokens = deviceTokens.stream()
                .filter(dt -> dt.getOsType() == OsType.IOS)
                .map(DeviceToken::getDeviceToken)
                .filter(this::isExpoPushToken)
                .toList();

        if (!androidTokens.isEmpty()) {
            sendAndroidByFcm(androidTokens, title, body, type, sourceId);
        }

        if (!iosExpoTokens.isEmpty()) {
            sendIosByExpo(iosExpoTokens, title, body, type, sourceId);
        }
    }

    private boolean isExpoPushToken(String t) {
        return t != null && t.startsWith("ExponentPushToken[");
    }

    private void sendAndroidByFcm(List<String> tokens,
                                  String title,
                                  String body,
                                  NotificationType type,
                                  Long sourceId) {
        final int LIMIT = 500;
        for (int i = 0; i < tokens.size(); i += LIMIT) {
            List<String> chunk = tokens.subList(i, Math.min(i + LIMIT, tokens.size()));

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setNotification(createNotification(title, body))
                    .setApnsConfig(createApnsConfig())
                    .setAndroidConfig(createAndroidConfig())
                    .putData("type", type.name())
                    .putData("id", String.valueOf(sourceId))
                    .build();

            try {
                retryTemplate.execute(ctx -> {
                    BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                    log.info("[FCM][Android] 성공: {}, 실패: {}", resp.getSuccessCount(), resp.getFailureCount());
                    for (int j = 0; j < resp.getResponses().size(); j++) {
                        var r = resp.getResponses().get(j);
                        if (!r.isSuccessful()) {
                            var ex = r.getException();
                            log.warn("[FCM][Android] 실패 token={}, code={}, msg={}",
                                    chunk.get(j), ex.getMessagingErrorCode(), ex.getMessage());
                        }
                    }
                    return null;
                });
            } catch (FirebaseMessagingException e) {
                log.error("[FCM][Android] 전송 실패: {}", e.getMessage(), e);
            }
        }
    }

    private final WebClient expoClient = WebClient.builder()
            .baseUrl("https://exp.host")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    private void sendIosByExpo(List<String> expoTokens,
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
                            "data", Map.of("type", type.name(), "id", String.valueOf(sourceId))
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

                    log.info("[Expo][iOS] 응답: {}", resp);
                    return null;
                });
            } catch (Exception e) {
                log.error("[Expo][iOS] 전송 실패: {}", e.getMessage(), e);
            }
        }
    }

    private Notification createNotification(String title, String body) {
        return Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
    }

    private ApnsConfig createApnsConfig() {
        Aps aps = Aps.builder()
                .setSound("default")
                .build();

        return ApnsConfig.builder()
                .setAps(aps)
                .build();
    }

    private AndroidConfig createAndroidConfig() {
        AndroidNotification androidNotification = AndroidNotification.builder()
                .setSound("default")
                .build();

        return AndroidConfig.builder()
                .setNotification(androidNotification)
                .build();
    }
}
