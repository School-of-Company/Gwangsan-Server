package team.startup.gwangsan.global.chat.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatting.server")
public record ChattingServerProperties(
        String url,
        String internalSecret
) {
    public boolean isEnabled() {
        return url != null && !url.isBlank();
    }
}
