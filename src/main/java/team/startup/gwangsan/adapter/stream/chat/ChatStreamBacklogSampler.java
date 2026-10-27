package team.startup.gwangsan.adapter.stream.chat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * consumer group backlog 를 주기적으로(기본 30초) 한 번 집계해 gauge 로 캐시한다. scrape 마다 Redis 를 훑지 않는다.
 * group 라벨은 stream 집합: rooms(chat:room:*:messages 합계), retry(retry stream).
 * unread=XINFO GROUPS lag 합(모든 lag 를 알고 전부 훑었을 때만), pending=전달됐지만 ACK 안 된 수(전부 훑었을 때만).
 * 모르면 NaN 으로 둔다. 0 으로 채우지 않는다.
 */
@Slf4j
@Component
public class ChatStreamBacklogSampler {

    static final String ROOMS = "rooms";
    static final String RETRY = "retry";
    private static final String STREAM_KEY_PATTERN = "chat:room:*:messages";

    private final StringRedisTemplate redisTemplate;
    private final ChatStreamProperties props;
    private final Map<String, Snapshot> snapshots = Map.of(ROOMS, new Snapshot(), RETRY, new Snapshot());

    public ChatStreamBacklogSampler(StringRedisTemplate redisTemplate, ChatStreamProperties props, MeterRegistry registry) {
        this.redisTemplate = redisTemplate;
        this.props = props;
        snapshots.forEach((group, snapshot) -> {
            gauge(registry, "gwangsan.stream.unread", group, snapshot, s -> s.unread);
            gauge(registry, "gwangsan.stream.pending", group, snapshot, s -> s.pending);
            gauge(registry, "gwangsan.stream.snapshot.timestamp.seconds", group, snapshot, s -> s.timestampSeconds);
            gauge(registry, "gwangsan.stream.snapshot.success", group, snapshot, s -> s.success);
            gauge(registry, "gwangsan.stream.snapshot.complete", group, snapshot, s -> s.complete);
        });
    }

    @Scheduled(fixedDelayString = "${chat.stream.snapshotMs:30000}", initialDelayString = "${chat.stream.snapshotInitialDelayMs:5000}")
    public void sample() {
        sample(ROOMS, this::scanRoomKeys);
        sample(RETRY, () -> new Keys(Boolean.TRUE.equals(redisTemplate.hasKey(props.getRetryKey()))
                ? List.of(props.getRetryKey()) : List.of(), false));
    }

    private void sample(String group, KeySource source) {
        Snapshot snapshot = snapshots.get(group);
        try {
            Keys keys = source.keys();
            long pending = 0;
            long unread = 0;
            boolean lagKnown = true;
            for (String key : keys.keys()) {
                StreamInfo.XInfoGroup info = findGroup(key);
                if (info == null) {
                    // 그룹이 아직 없으면 전달 전 항목 수를 lag 로 알 수 없다.
                    lagKnown = false;
                    continue;
                }
                pending += info.pendingCount() == null ? 0 : info.pendingCount();
                Object lag = info.getRaw().get("lag");
                if (lag instanceof Number number) {
                    unread += number.longValue();
                } else {
                    lagKnown = false; // tombstone·trim 등으로 Redis 가 lag 를 계산하지 못한 경우(null)
                }
            }
            boolean covered = !keys.truncated();
            snapshot.pending = covered ? pending : Double.NaN;
            snapshot.unread = covered && lagKnown ? unread : Double.NaN;
            snapshot.complete = covered && lagKnown ? 1 : 0;
            snapshot.success = 1;
            snapshot.timestampSeconds = System.currentTimeMillis() / 1000.0;
        } catch (Exception e) {
            snapshot.unread = Double.NaN;
            snapshot.pending = Double.NaN;
            snapshot.complete = 0;
            snapshot.success = 0;
            log.warn("[ChatStream] backlog snapshot failed. group={}, error_type={}", group, e.getClass().getName());
        }
    }

    private StreamInfo.XInfoGroup findGroup(String key) {
        for (StreamInfo.XInfoGroup info : redisTemplate.opsForStream().groups(key)) {
            if (props.getGroup().equals(info.groupName())) return info;
        }
        return null;
    }

    private Keys scanRoomKeys() {
        int max = props.getSnapshotMaxKeys();
        Set<String> keys = new LinkedHashSet<>(); // SCAN 은 같은 key 를 두 번 줄 수 있다
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions().match(STREAM_KEY_PATTERN).count(100).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (keys.size() >= max) return new Keys(keys, true);
                keys.add(key);
            }
        }
        return new Keys(keys, false);
    }

    private static void gauge(MeterRegistry registry, String name, String group, Snapshot snapshot,
                              ToDoubleFunction<Snapshot> value) {
        Gauge.builder(name, snapshot, value).tag("group", group).strongReference(true).register(registry);
    }

    private record Keys(Collection<String> keys, boolean truncated) {}

    @FunctionalInterface
    private interface KeySource {
        Keys keys();
    }

    private static final class Snapshot {
        volatile double unread = Double.NaN;
        volatile double pending = Double.NaN;
        volatile double timestampSeconds = Double.NaN;
        volatile double success = 0;
        volatile double complete = 0;
    }
}
