package team.startup.gwangsan.global.thirdparty.firebase;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.notification.NotificationPort;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmAdapter implements NotificationPort {

    private final RetryTemplate retryTemplate;

    @Override
    public void sendNotification(List<String> deviceTokens, String title, String body) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setNotification(createNotification(title, body))
                .setApnsConfig(createApnsConfig())
                .setAndroidConfig(createAndroidConfig())
                .build();

        try {
            retryTemplate.execute(context -> {
                FirebaseMessaging.getInstance().sendEachForMulticast(message);
                return null;
            });
        } catch (FirebaseMessagingException e) {
            log.error("FCM 알림 보내기 실패했습니다. : {}", e.getMessage());
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
