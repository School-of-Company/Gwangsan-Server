package team.startup.gwangsan.global.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("jwt")
public class JwtProperties {
    private String accessSecret;
    private String refreshSecret;
}

