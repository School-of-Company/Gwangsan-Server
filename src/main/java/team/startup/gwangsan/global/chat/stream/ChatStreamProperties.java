package team.startup.gwangsan.global.chat.stream;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "chat.stream")
public class ChatStreamProperties {
    private String group;
    private int batchSize;
    private long blockMs;
    private long claimIdleMs;
    private int retryMax;
    private String retryKey;
    private String dlqKey;
}
