package team.startup.gwangsan.adapter.stream.chat;

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
    // backlog snapshot 한 번에 보는 방 stream 최대 수. 넘으면 부분 집계로 표시한다.
    private int snapshotMaxKeys = 1000;
}
