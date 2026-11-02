package team.startup.gwangsan.adapter.stream.chat;

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
                boolean retryStream = streamKey.equals(props.getRetryKey());
                for (MapRecord<String, String, String> record : records) {
                    if (retryStream && !isRetryDue(record)) continue; // ACK 없이 pending 에 남겨 reclaim 이 처리한다
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

    /**
     * retry record 는 retry stream 에 들어간 지 claimIdleMs 가 지나야 다시 시도한다. 바로 다시 읽으면
     * retryMax 회가 몇 초 안에 소진돼 짧은 DB 장애에도 DLQ 로 간다(#464). 아직 이른 record 는 pending 에
     * 남고 reclaimPendingMessages 가 idle 시간이 지난 뒤 가져간다.
     * ponytail: 시도 간격은 claimIdleMs~claimIdleMs+60s 고정. attempt 별 지수 백오프가 필요하면 여기서 계산.
     */
    private boolean isRetryDue(MapRecord<String, String, String> record) {
        return System.currentTimeMillis() - record.getId().getTimestamp() >= props.getClaimIdleMs();
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
