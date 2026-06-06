package team.startup.gwangsan.global.thirdparty.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai.server")
public record AiModerationProperties(
        String url,
        Duration connectTimeout,
        Duration readTimeout
) {
    public boolean isEnabled() {
        return url != null && !url.isBlank();
    }
}
