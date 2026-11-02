package team.startup.gwangsan.infrastructure.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SDK 는 OpenTelemetry Java agent 가 등록한 global 하나뿐이다. 앱에서 두 번째 SDK 를 만들지 않는다.
 * agent 없이 실행하면 no-op 이라 span 은 버려지고 업무 동작은 그대로다.
 */
@Configuration
public class OpenTelemetryConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }
}
