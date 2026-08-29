package team.startup.gwangsan.global.chat.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "chatting.server")
public record ChattingServerProperties(
        String url,
        String internalSecret,
        Duration connectTimeout,
        Duration readTimeout
) {
    public boolean isEnabled() {
        return url != null && !url.isBlank();
    }
}
