package team.startup.gwangsan.global.chat.stream;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.service.impl.SaveChatMessageServiceImpl;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.OsType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;
import team.startup.gwangsan.global.util.BlockValidator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.show-sql=false"})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(RedisAutoConfiguration.class)
@Import({QueryDslConfig.class, SaveChatMessageServiceImpl.class, BlockValidator.class,
        ChatMessageIdempotencyIntegrationTest.EventConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChatMessageIdempotencyIntegrationTest {
    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @TestConfiguration
    @EnableRedisRepositories(basePackageClasses = DeviceTokenRepository.class)
    static class EventConfiguration {
        @Bean
        CommittedEvents committedEvents() { return new CommittedEvents(); }
    }

    static class CommittedEvents {
        final AtomicInteger count = new AtomicInteger();
        @TransactionalEventListener
        public void receive(SendNotificationEvent event) { count.incrementAndGet(); }
    }

    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired SaveChatMessageServiceImpl service;
    @Autowired DeviceTokenRepository deviceTokens;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired JdbcTemplate jdbc;
    @Autowired CommittedEvents events;
    private Fixture fixture;
    private ChatStreamProperties props;
    private ChatStreamConsumerWorker worker;
    private ChatStreamRedisAdapter adapter;
    private ChatStreamMessageProcessor processor;
    private String stream;

    @BeforeEach
    void setUp() {
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
        fixture = new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString();
            Member buyer = member("buyer-" + suffix);
            Member seller = member("seller-" + suffix);
            entityManager.persist(buyer);
            entityManager.persist(seller);
            ChatRoom room = ChatRoom.builder().buyer(buyer).seller(seller)
                    .createdAt(LocalDateTime.now()).isActive(true).build();
            entityManager.persist(room);
            Image image = Image.builder().imageUrl("https://example.test/image").build();
            entityManager.persist(image);
            return new Fixture(buyer.getId(), seller.getId(), room.getId(), image.getId());
        });
        deviceTokens.save(DeviceToken.builder().deviceId("test-device").userId(fixture.sellerId())
                .deviceToken("test-token").osType(OsType.ANDROID).build());
        events.count.set(0);
        stream = "chat:room:" + fixture.roomId() + ":messages";
        props = new ChatStreamProperties();
        props.setGroup("idempotency-test");
        props.setBatchSize(20);
        props.setClaimIdleMs(0);
        props.setRetryMax(5);
        props.setRetryKey("chat:retry:messages");
        props.setDlqKey("chat:dlq:messages");
        adapter = new ChatStreamRedisAdapter(redisTemplate, props, new Gson());
        adapter.init();
        processor = new ChatStreamMessageProcessor(List.of(new SaveChatMessageHandler(service)), adapter, props, new Gson());
        worker = new ChatStreamConsumerWorker(adapter, processor, props);
    }

    @Test
    void sequential_text_replay_preserves_checked() {
        publish(1001L, "TEXT");
        worker.consumeMessages();
        assertThat(messageCount(1001L)).isEqualTo(1);
        jdbc.update("UPDATE tbl_chat_message SET checked=true WHERE message_id=?", 1001L);
        publish(1001L, "TEXT");
        worker.consumeMessages();
        assertThat(jdbc.queryForObject("SELECT checked FROM tbl_chat_message WHERE message_id=?", Boolean.class, 1001L)).isTrue();
    }

    @Test
    void sequential_image_replay_does_not_add_links() {
        publish(1002L, "IMAGE");
        worker.consumeMessages();
        assertThat(linkCount(1002L)).isEqualTo(1);
        for (int i = 0; i < 9; i++) publish(1002L, "IMAGE");
        worker.consumeMessages();
        assertThat(linkCount(1002L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void sequential_replay_does_not_publish_another_committed_notification() {
        publish(1003L, "TEXT");
        worker.consumeMessages();
        assertThat(events.count.get()).isEqualTo(1);
        for (int i = 0; i < 9; i++) publish(1003L, "TEXT");
        worker.consumeMessages();
        assertThat(messageCount(1003L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void eight_concurrent_stream_records_with_same_id_commit_once() throws Exception {
        for (int i = 0; i < 8; i++) publish(2001L, "IMAGE");
        processConcurrently();
        assertThat(messageCount(2001L)).isEqualTo(1);
        assertThat(linkCount(2001L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
        assertThat(pendingCount()).isZero();
        assertThat(redisTemplate.opsForStream().size(props.getRetryKey())).isZero();
        assertThat(redisTemplate.hasKey(props.getDlqKey())).isFalse();
    }

    @Test
    void different_ids_commit_independently() throws Exception {
        for (int i = 0; i < 8; i++) publish(2100L + i, "TEXT");
        processConcurrently();
        for (int i = 0; i < 8; i++) assertThat(messageCount(2100L + i)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(8);
        assertThat(pendingCount()).isZero();
    }

    @Test
    void conflicting_payloads_go_to_dlq_without_mutation_or_events() {
        publish(2200L, "IMAGE");
        worker.consumeMessages();
        for (Map<String, String> difference : List.of(
                Map.of("roomId", "999999"), Map.of("senderId", fixture.sellerId().toString()),
                Map.of("content", "different"), Map.of("messageType", "TEXT"),
                Map.of("createdAt", "1700000000124"), Map.of("imageIds", "[]"))) {
            Map<String, String> body = payload(2200L, "IMAGE");
            body.putAll(difference);
            publish(body);
        }
        worker.consumeMessages();
        assertThat(redisTemplate.opsForStream().size(props.getDlqKey())).isEqualTo(6);
        assertThat(redisTemplate.opsForStream().size(props.getRetryKey())).isZero();
        assertThat(messageCount(2200L)).isEqualTo(1);
        assertThat(linkCount(2200L)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT content FROM tbl_chat_message WHERE message_id=2200", String.class))
                .isEqualTo("message");
        assertThat(events.count.get()).isEqualTo(1);
        assertThat(pendingCount()).isZero();
    }

    @Test
    void replay_after_block_and_device_change_keeps_committed_result() {
        publish(2300L, "TEXT");
        worker.consumeMessages();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> entityManager.persist(
                MemberBlock.builder().blocker(entityManager.getReference(Member.class, fixture.sellerId()))
                        .blocked(entityManager.getReference(Member.class, fixture.buyerId())).build()));
        deviceTokens.deleteAll();
        publish(2300L, "TEXT");
        worker.consumeMessages();
        assertThat(messageCount(2300L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
        assertThat(redisTemplate.hasKey(props.getDlqKey())).isFalse();
        assertThat(pendingCount()).isZero();
    }

    @Test
    void hidden_states_survive_replay_in_both_directions() {
        LocalDateTime hiddenAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ChatRoom room = entityManager.find(ChatRoom.class, fixture.roomId());
            room.hideFor(room.getBuyer(), hiddenAt);
            room.hideFor(room.getSeller(), hiddenAt.plusHours(1));
        });
        deviceTokens.save(DeviceToken.builder().deviceId("buyer-device").userId(fixture.buyerId())
                .deviceToken("buyer-token").osType(OsType.IOS).build());
        for (int i = 0; i < 2; i++) {
            publish(2400L, "TEXT");
            Map<String, String> sellerMessage = payload(2401L, "TEXT");
            sellerMessage.put("senderId", fixture.sellerId().toString());
            publish(sellerMessage);
            worker.consumeMessages();
        }
        assertThat(messageCount(2400L)).isEqualTo(1);
        assertThat(messageCount(2401L)).isEqualTo(1);
        assertThat(events.count.get()).isZero();
        assertThat(jdbc.queryForObject("SELECT hidden_by_buyer_at FROM tbl_chat_room WHERE room_id=?",
                LocalDateTime.class, fixture.roomId())).isEqualTo(hiddenAt);
        assertThat(jdbc.queryForObject("SELECT hidden_by_seller_at FROM tbl_chat_room WHERE room_id=?",
                LocalDateTime.class, fixture.roomId())).isEqualTo(hiddenAt.plusHours(1));
    }

    @Test
    void failure_after_real_image_writes_rolls_back_everything_and_can_retry() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            saveImage(2500L);
            entityManager.flush();
            assertThat(linkCount(2500L)).isEqualTo(1);
            throw new IllegalStateException("injected failure after image flush");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(messageCount(2500L)).isZero();
        assertThat(linkCount(2500L)).isZero();
        assertThat(events.count.get()).isZero();
        saveImage(2500L);
        assertThat(messageCount(2500L)).isEqualTo(1);
        assertThat(linkCount(2500L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void concurrent_retry_acquires_first_processing_after_winner_rolls_back() throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch contenderStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> winner = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                saveImage(2600L);
                entityManager.flush();
                inserted.countDown();
                await(contenderStarted);
                status.setRollbackOnly();
            }));
            Future<?> contender = executor.submit(() -> {
                await(inserted);
                contenderStarted.countDown();
                saveImage(2600L);
            });
            winner.get(20, TimeUnit.SECONDS);
            contender.get(20, TimeUnit.SECONDS);
        }
        assertThat(messageCount(2600L)).isEqualTo(1);
        assertThat(linkCount(2600L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void committed_message_survives_real_ack_failure_and_new_consumer_claim() throws Exception {
        publish(2700L, "IMAGE");
        acl("-xack");
        try {
            worker.consumeMessages();
            assertThat(messageCount(2700L)).isEqualTo(1);
            assertThat(linkCount(2700L)).isEqualTo(1);
            assertThat(events.count.get()).isEqualTo(1);
            assertThat(pendingCount()).isEqualTo(1);
        } finally {
            acl("+xack");
        }
        ChatStreamRedisAdapter restarted = new ChatStreamRedisAdapter(redisTemplate, props, new Gson());
        restarted.init();
        ReflectionTestUtils.setField(restarted, "consumerName", "restarted-consumer");
        restarted.refreshStreamKeys();
        List<MapRecord<String, String, String>> claimed = restarted.claimIdleRecords(stream);
        assertThat(claimed).hasSize(1);
        assertThat(redisTemplate.opsForStream().pending(stream, props.getGroup(), Range.unbounded(), 10L)
                .iterator().next().getConsumerName()).isEqualTo("restarted-consumer");
        ChatStreamMessageProcessor restartedProcessor = new ChatStreamMessageProcessor(
                List.of(new SaveChatMessageHandler(service)), restarted, props, new Gson());
        ChatStreamConsumerWorker restartedWorker = new ChatStreamConsumerWorker(restarted, restartedProcessor, props);
        restartedWorker.reclaimPendingMessages();
        assertThat(pendingCount()).isZero();
        assertThat(messageCount(2700L)).isEqualTo(1);
        assertThat(linkCount(2700L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void failed_dlq_handoff_leaves_original_pending_until_recovery() throws Exception {
        publish(2800L, "TEXT");
        worker.consumeMessages();
        Map<String, String> conflict = payload(2800L, "TEXT");
        conflict.put("content", "conflict");
        publish(conflict);
        acl("-xadd");
        try {
            worker.consumeMessages();
            assertThat(pendingCount()).isEqualTo(1);
            assertThat(redisTemplate.hasKey(props.getDlqKey())).isFalse();
        } finally {
            acl("+xadd");
        }
        worker.reclaimPendingMessages();
        assertThat(pendingCount()).isZero();
        assertThat(redisTemplate.opsForStream().size(props.getDlqKey())).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void failed_retry_handoff_leaves_original_pending_until_recovery() throws Exception {
        publish(2900L, "TEXT");
        ChatStreamHandler failureAfterSave = message -> {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                new SaveChatMessageHandler(service).handle(message);
                entityManager.flush();
                throw new IllegalStateException("injected transient failure");
            });
        };
        ChatStreamMessageProcessor failingProcessor = new ChatStreamMessageProcessor(
                List.of(failureAfterSave), adapter, props, new Gson());
        ChatStreamConsumerWorker failingWorker = new ChatStreamConsumerWorker(adapter, failingProcessor, props);
        acl("-xadd");
        try {
            failingWorker.consumeMessages();
            assertThat(messageCount(2900L)).isZero();
            assertThat(events.count.get()).isZero();
            assertThat(pendingCount()).isEqualTo(1);
            assertThat(redisTemplate.opsForStream().size(props.getRetryKey())).isZero();
        } finally {
            acl("+xadd");
        }
        worker.reclaimPendingMessages();
        assertThat(pendingCount()).isZero();
        assertThat(messageCount(2900L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void killed_database_connection_rolls_back_message_and_images() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            saveImage(3000L);
            entityManager.flush();
            Long connectionId = jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class);
            try {
                var result = mariadb.execInContainer("mariadb", "-uroot", "-p" + mariadb.getPassword(),
                        "-e", "KILL CONNECTION " + connectionId);
                assertThat(result.getExitCode()).isZero();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            jdbc.queryForObject("SELECT 1", Integer.class);
        })).isInstanceOf(RuntimeException.class);
        assertThat(messageCount(3000L)).isZero();
        assertThat(linkCount(3000L)).isZero();
        assertThat(events.count.get()).isZero();
        saveImage(3000L);
        assertThat(messageCount(3000L)).isEqualTo(1);
        assertThat(linkCount(3000L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void repeated_and_missing_image_ids_keep_existing_image_set_semantics() {
        Map<String, String> first = payload(3100L, "IMAGE");
        first.put("imageIds", "[" + fixture.imageId() + "," + fixture.imageId() + ",999999]");
        publish(first);
        worker.consumeMessages();
        publish(3100L, "IMAGE");
        worker.consumeMessages();
        assertThat(linkCount(3100L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
        assertThat(redisTemplate.hasKey(props.getDlqKey())).isFalse();
    }

    @Test
    void blocked_first_processing_leaves_no_partial_message() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> entityManager.persist(
                MemberBlock.builder().blocker(entityManager.getReference(Member.class, fixture.buyerId()))
                        .blocked(entityManager.getReference(Member.class, fixture.sellerId())).build()));
        publish(3200L, "IMAGE");
        worker.consumeMessages();
        assertThat(messageCount(3200L)).isZero();
        assertThat(linkCount(3200L)).isZero();
        assertThat(events.count.get()).isZero();
        assertThat(redisTemplate.opsForStream().size(props.getDlqKey())).isEqualTo(1);
        assertThat(pendingCount()).isZero();
    }

    @Test
    void duplicate_after_stale_snapshot_reads_committed_images_and_checked() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(messageCount(3300L)).isZero();
            try (var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(() -> {
                    saveImage(3300L);
                    jdbc.update("UPDATE tbl_chat_message SET checked=true WHERE message_id=3300");
                }).get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            var replay = service.execute(3300L, fixture.roomId(), "message", List.of(fixture.imageId()),
                    MessageType.IMAGE, fixture.buyerId(),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(1700000000123L), ZoneId.systemDefault()));
            assertThat(replay.checked()).isTrue();
            assertThat(replay.images()).hasSize(1);
            assertThat(replay.images().getFirst().imageId()).isEqualTo(fixture.imageId());
        });
        assertThat(events.count.get()).isEqualTo(1);
        assertThat(linkCount(3300L)).isEqualTo(1);
    }

    @Test
    void concurrent_conflicting_payloads_keep_one_winner_and_quarantine_losers() throws Exception {
        for (int i = 0; i < 8; i++) {
            Map<String, String> body = payload(3400L, "IMAGE");
            body.put("content", i % 2 == 0 ? "first variant" : "second variant");
            publish(body);
        }
        processConcurrently();
        assertThat(messageCount(3400L)).isEqualTo(1);
        assertThat(linkCount(3400L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
        assertThat(redisTemplate.opsForStream().size(props.getDlqKey())).isEqualTo(4);
        assertThat(redisTemplate.opsForStream().size(props.getRetryKey())).isZero();
        assertThat(pendingCount()).isZero();
    }

    @Test
    void image_null_content_replays_and_nonnull_content_conflicts() {
        Map<String, String> body = payload(3500L, "IMAGE");
        body.remove("content");
        publish(body);
        worker.consumeMessages();
        publish(body);
        worker.consumeMessages();
        assertThat(linkCount(3500L)).isEqualTo(1);
        assertThat(events.count.get()).isEqualTo(1);
        assertThat(redisTemplate.opsForStream().size(props.getDlqKey())).isZero();
        body.put("content", "now has text");
        publish(body);
        worker.consumeMessages();
        assertThat(redisTemplate.opsForStream().size(props.getDlqKey())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT content FROM tbl_chat_message WHERE message_id=3500", String.class)).isNull();
        assertThat(events.count.get()).isEqualTo(1);
    }

    @Test
    void missing_member_or_room_leaves_no_partial_data() {
        Map<String, String> missingMember = payload(3600L, "IMAGE");
        missingMember.put("senderId", "999999");
        Map<String, String> missingRoom = payload(3601L, "IMAGE");
        missingRoom.put("roomId", "999999");
        publish(missingMember);
        publish(missingRoom);
        worker.consumeMessages();
        for (long id : List.of(3600L, 3601L)) {
            assertThat(messageCount(id)).isZero();
            assertThat(linkCount(id)).isZero();
        }
        assertThat(events.count.get()).isZero();
        assertThat(redisTemplate.opsForStream().size(props.getDlqKey())).isEqualTo(2);
        assertThat(pendingCount()).isZero();
    }

    private void processConcurrently() throws Exception {
        adapter.ensureGroupExists(stream);
        List<MapRecord<String, String, String>> records = adapter.readMessages(stream);
        assertThat(records).hasSize(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<?>> futures = new ArrayList<>();
            for (MapRecord<String, String, String> record : records) futures.add(executor.submit(() -> {
                ready.countDown();
                await(start);
                processor.process(stream, record, 0);
            }));
            await(ready);
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        }
    }

    private void saveImage(long id) {
        service.execute(id, fixture.roomId(), "message", List.of(fixture.imageId()), MessageType.IMAGE,
                fixture.buyerId(), LocalDateTime.ofInstant(Instant.ofEpochMilli(1700000000123L), ZoneId.systemDefault()));
    }

    private long pendingCount() {
        return redisTemplate.opsForStream().pending(stream, props.getGroup()).getTotalPendingMessages();
    }

    private void acl(String rule) throws Exception {
        var result = redis.execInContainer("redis-cli", "ACL", "SETUSER", "default", rule);
        assertThat(result.getExitCode()).isZero();
        assertThat(result.getStdout().trim()).isEqualTo("OK");
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void publish(long id, String type) {
        publish(payload(id, type));
    }

    private Map<String, String> payload(long id, String type) {
        return new LinkedHashMap<>(Map.of(
                "messageId", Long.toString(id), "roomId", fixture.roomId().toString(),
                "senderId", fixture.buyerId().toString(), "content", "message", "messageType", type,
                "createdAt", "1700000000123", "imageIds", "[" + fixture.imageId() + "]"));
    }

    private void publish(Map<String, String> body) {
        redisTemplate.opsForStream().add(StreamRecords.newRecord().in(stream).ofMap(body));
    }

    private int messageCount(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_chat_message WHERE message_id=?", Integer.class, id);
    }

    private int linkCount(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_chat_message_image WHERE message_id=?", Integer.class, id);
    }

    private Member member(String name) {
        return Member.builder().name(name).nickname(name).phoneNumber(name).password("pw")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
    }

    private record Fixture(Long buyerId, Long sellerId, Long roomId, Long imageId) { }
}
