package team.startup.gwangsan.global.chat.stream;

import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamRedisAdapter {

    private static final String STREAM_KEY_PATTERN = "chat:room:*:messages";

    private final StringRedisTemplate redisTemplate;
    private final ChatStreamProperties props;
    private final Gson gson;
    private String consumerName;
    private final Set<String> knownStreamKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> initializedGroups = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        String instanceId = System.getenv("INSTANCE_ID");
        if (instanceId != null && !instanceId.isBlank()) {
            consumerName = instanceId;
        } else {
            try {
                consumerName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                consumerName = "consumer-" + UUID.randomUUID().toString().substring(0, 8);
            }
        }
    }

    public void refreshStreamKeys() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(STREAM_KEY_PATTERN)
                .count(100)
                .build();
        Set<String> freshKeys = ConcurrentHashMap.newKeySet();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(freshKeys::add);
        } catch (Exception e) {
            log.error("[ChatStream] Failed to scan stream keys: {}", e.getMessage());
            return;
        }
        knownStreamKeys.retainAll(freshKeys);
        knownStreamKeys.addAll(freshKeys);
        initializedGroups.retainAll(freshKeys);
    }

    public Set<String> getKnownStreamKeys() {
        return Collections.unmodifiableSet(knownStreamKeys);
    }

    public void ensureGroupExists(String streamKey) {
        if (initializedGroups.contains(streamKey)) return;
        try {
            streamOps().createGroup(streamKey, ReadOffset.from("0"), props.getGroup());
            initializedGroups.add(streamKey);
        } catch (Exception e) {
            log.debug("[ChatStream] Group '{}' already exists or stream missing: {}", props.getGroup(), e.getMessage());
            initializedGroups.add(streamKey);
        }
    }

    @SuppressWarnings("unchecked")
    public List<MapRecord<String, String, String>> readMessages(String streamKey) {
        return streamOps().read(
                Consumer.from(props.getGroup(), consumerName),
                StreamReadOptions.empty().count(props.getBatchSize()),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        );
    }

    public List<MapRecord<String, String, String>> claimIdleRecords(String streamKey) {
        PendingMessages pending = streamOps().pending(
                streamKey, props.getGroup(), Range.unbounded(), 100L
        );
        List<MapRecord<String, String, String>> result = new ArrayList<>();
        for (PendingMessage msg : pending) {
            if (msg.getElapsedTimeSinceLastDelivery().toMillis() < props.getClaimIdleMs()) {
                continue;
            }
            try {
                List<MapRecord<String, String, String>> claimed = claimSingle(streamKey, msg.getId());
                result.addAll(claimed);
            } catch (Exception e) {
                log.error("[ChatStream] Claim failed for {} in {}: {}", msg.getId(), streamKey, e.getMessage());
            }
        }
        return result;
    }

    public void ack(String streamKey, RecordId recordId) {
        try {
            streamOps().acknowledge(streamKey, props.getGroup(), recordId);
        } catch (Exception e) {
            log.error("[ChatStream] ACK failed for {} in {}: {}", recordId, streamKey, e.getMessage());
        }
    }

    public void sendToRetry(String originalKey, MapRecord<String, String, String> record,
                             int attempt, String errorMessage) {
        Map<String, String> fields = new LinkedHashMap<>(record.getValue());
        fields.put(ChatStreamField.ATTEMPT, String.valueOf(attempt));
        fields.putIfAbsent("originalStreamKey", originalKey);
        fields.putIfAbsent("originalStreamId", record.getId().getValue());
        fields.put("errorMessage", truncate(errorMessage, 500));
        streamOps().add(StreamRecords.newRecord().in(props.getRetryKey()).ofMap(fields));
    }

    public void sendToDlq(String originalKey, MapRecord<String, String, String> record,
                           String errorMessage) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("originalStreamKey", originalKey);
        fields.put("originalStreamId", record.getId().getValue());
        fields.put("payload", gson.toJson(record.getValue()));
        fields.put("errorMessage", truncate(errorMessage, 500));
        fields.put("failedAt", String.valueOf(System.currentTimeMillis()));
        streamOps().add(StreamRecords.newRecord().in(props.getDlqKey()).ofMap(fields));
    }

    @SuppressWarnings("unchecked")
    private List<MapRecord<String, String, String>> claimSingle(String streamKey, RecordId recordId) {
        return (List<MapRecord<String, String, String>>) (List<?>) streamOps().claim(
                streamKey, props.getGroup(), consumerName,
                Duration.ofMillis(props.getClaimIdleMs()),
                recordId
        );
    }

    private StreamOperations<String, String, String> streamOps() {
        return redisTemplate.opsForStream();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
