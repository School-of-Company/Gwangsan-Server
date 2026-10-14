package team.startup.gwangsan.adapter.http.chat;

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
