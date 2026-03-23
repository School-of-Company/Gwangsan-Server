package team.startup.gwangsan.global.chat.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamConsumerWorker {

    private final ChatStreamRedisAdapter redisAdapter;
    private final ChatStreamMessageProcessor processor;
    private final ChatStreamProperties props;

    @Scheduled(fixedDelayString = "${chat.stream.blockMs:2000}")
    public void consumeMessages() {
        redisAdapter.refreshStreamKeys();

        Set<String> allKeys = new HashSet<>(redisAdapter.getKnownStreamKeys());
        allKeys.add(props.getRetryKey());

        for (String streamKey : allKeys) {
            try {
                redisAdapter.ensureGroupExists(streamKey);
                List<MapRecord<String, String, String>> records = redisAdapter.readMessages(streamKey);
                if (records == null) continue;
                for (MapRecord<String, String, String> record : records) {
                    int attempt = parseAttempt(record.getValue());
                    processor.process(streamKey, record, attempt);
                }
            } catch (Exception e) {
                log.error("[ChatStream] Error processing stream {}: {}", streamKey, e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void reclaimPendingMessages() {
        Set<String> allKeys = new HashSet<>(redisAdapter.getKnownStreamKeys());
        allKeys.add(props.getRetryKey());

        for (String streamKey : allKeys) {
            try {
                List<MapRecord<String, String, String>> claimed = redisAdapter.claimIdleRecords(streamKey);
                for (MapRecord<String, String, String> record : claimed) {
                    int attempt = parseAttempt(record.getValue());
                    processor.process(streamKey, record, attempt);
                }
            } catch (Exception e) {
                log.error("[ChatStream] Error reclaiming pending for {}: {}", streamKey, e.getMessage());
            }
        }
    }

    private int parseAttempt(Map<String, String> body) {
        String val = body.get(ChatStreamField.ATTEMPT);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
