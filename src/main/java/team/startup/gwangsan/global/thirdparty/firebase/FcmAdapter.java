package team.startup.gwangsan.global.thirdparty.firebase;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.notification.NotificationPort;

import java.util.List;

@Component
public class FcmAdapter implements NotificationPort {

    @Override
    public void sendNotification(List<String> deviceTokens, String title, String body) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setNotification(createNotification(title, body))
                .setApnsConfig(createApnsConfig())
                .setAndroidConfig(createAndroidConfig())
                .build();

        FirebaseMessaging.getInstance().sendEachForMulticastAsync(message);
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
