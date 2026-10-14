package team.startup.gwangsan.adapter.sms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("sms")
public class SmsProperties {
    private String apiKey;
    private String apiSecret;
    private String fromNumber;
}