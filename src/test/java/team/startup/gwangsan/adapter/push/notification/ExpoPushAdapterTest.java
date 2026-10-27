package team.startup.gwangsan.adapter.push.notification;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import team.startup.gwangsan.infrastructure.config.RetryConfig;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.domain.notification.model.PushOutcome;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("ExpoPushAdapter (local HTTP stub)")
class ExpoPushAdapterTest {

    private static final String T1 = "ExponentPushToken[aaa]";
    private static final String T2 = "ExponentPushToken[bbb]";
    private static final PushMessage CHAT = PushMessage.byType(NotificationType.CHATTING, 5L);

    @Test
    @DisplayName("문의 답변 푸시는 feedbackId와 알림 유형을 실제 HTTP data로 전송한다")
    void it_sends_feedback_navigation_data() {
        respond(200, ok(1));
        assertThat(adapter.send(PushMessage.byType(NotificationType.FEEDBACK_ANSWER, 449L), List.of(T1)))
                .isEqualTo(PushOutcome.SENT);
        var data = JsonParser.parseString(requests.getFirst()).getAsJsonArray().get(0)
                .getAsJsonObject().getAsJsonObject("data");
        assertThat(data.get("alertType").getAsString()).isEqualTo("FEEDBACK_ANSWER");
        assertThat(data.get("feedbackId").getAsString()).isEqualTo("449");
        assertThat(data.get("sourceId").getAsString()).isEqualTo("449");
    }

    private HttpServer server;
    private final Deque<int[]> statuses = new ArrayDeque<>();
    private final Deque<String> bodies = new ArrayDeque<>();
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
    private ExpoPushAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/--/api/v2/push/send", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int status = statuses.isEmpty() ? 200 : statuses.poll()[0];
            String body = bodies.isEmpty() ? "{\"data\":[]}" : bodies.poll();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        // 기존 공용 RetryTemplate 과 같은 3회 시도, 테스트 시간만 줄이기 위해 backoff 1ms
        RetryTemplate retry = RetryTemplate.builder().maxAttempts(3).fixedBackoff(1).build();
        adapter = new ExpoPushAdapter(retry, WebClient.builder(), new Gson(),
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(int status, String body) {
        statuses.add(new int[]{status});
        bodies.add(body);
    }

    private static String ok(int n) {
        return "{\"data\":[" + String.join(",", Collections.nCopies(n, "{\"status\":\"ok\",\"id\":\"x\"}")) + "]}";
    }

    private static String ticketError(String error) {
        return "{\"status\":\"error\",\"message\":\"m\",\"details\":{\"error\":\"" + error + "\"}}";
    }

    private List<String> sentTokens(int request) {
        JsonArray payload = JsonParser.parseString(requests.get(request)).getAsJsonArray();
        List<String> tokens = new ArrayList<>();
        payload.forEach(e -> tokens.add(e.getAsJsonObject().get("to").getAsString()));
        return tokens;
    }

    @Nested
    @DisplayName("발송 대상이 없으면")
    class Describe_no_target {

        @Test
        @DisplayName("빈 목록·null 유형·Expo 형식이 아닌 토큰만 있을 때 요청 없이 NO_TARGET")
        void it_skips_request() {
            assertThat(adapter.send(CHAT, List.of())).isEqualTo(PushOutcome.NO_TARGET);
            assertThat(adapter.send(CHAT, null)).isEqualTo(PushOutcome.NO_TARGET);
            assertThat(adapter.send(new PushMessage("t", "b", null, 1L), List.of(T1))).isEqualTo(PushOutcome.NO_TARGET);
            assertThat(adapter.send(CHAT, List.of("fcm-native-token"))).isEqualTo(PushOutcome.NO_TARGET);
            assertThat(requests).isEmpty();
        }
    }

    @Nested
    @DisplayName("정상 응답이면")
    class Describe_success {

        @Test
        @DisplayName("기존 payload(to,title,body,sound,data) 로 한 번 보내고 SENT")
        void it_sends_existing_payload() {
            respond(200, ok(2));

            assertThat(adapter.send(CHAT, List.of(T1, T2, T1, "native"))).isEqualTo(PushOutcome.SENT);

            assertThat(requests).hasSize(1);
            var first = JsonParser.parseString(requests.getFirst()).getAsJsonArray().get(0).getAsJsonObject();
            assertThat(first.get("to").getAsString()).isEqualTo(T1);
            assertThat(first.get("title").getAsString()).isEqualTo("시민화폐 광산");
            assertThat(first.get("body").getAsString()).isEqualTo("새로운 메세지가 도착했습니다.");
            assertThat(first.get("sound").getAsString()).isEqualTo("default");
            assertThat(first.getAsJsonObject("data").get("roomId").getAsString()).isEqualTo("5");
            assertThat(sentTokens(0)).as("중복·비Expo 토큰 제외").containsExactly(T1, T2);
        }

        @Test
        @DisplayName("101개 토큰은 100개와 1개 청크로 나눠 보낸다")
        void it_chunks_by_100() {
            List<String> tokens = IntStream.range(0, 101).mapToObj(i -> "ExponentPushToken[" + i + "]").toList();
            respond(200, ok(100));
            respond(200, ok(1));

            assertThat(adapter.send(CHAT, tokens)).isEqualTo(PushOutcome.SENT);
            assertThat(requests).hasSize(2);
            assertThat(sentTokens(0)).hasSize(100);
            assertThat(sentTokens(1)).containsExactly("ExponentPushToken[100]");
        }
    }

    @Nested
    @DisplayName("일시 실패는")
    class Describe_transient {

        @Test
        @DisplayName("HTTP 5xx 와 429 를 재시도해 성공하면 SENT")
        void it_retries_5xx_and_429() {
            respond(503, "unavailable");
            respond(429, "slow down");
            respond(200, ok(1));

            assertThat(adapter.send(CHAT, List.of(T1))).isEqualTo(PushOutcome.SENT);
            assertThat(requests).hasSize(3);
        }

        @Test
        @DisplayName("기존 횟수(3회)를 넘으면 FAILED")
        void it_gives_up_after_three_attempts() {
            respond(500, "e");
            respond(500, "e");
            respond(500, "e");

            assertThat(adapter.send(CHAT, List.of(T1))).isEqualTo(PushOutcome.FAILED);
            assertThat(requests).hasSize(3);
        }

        @Test
        @DisplayName("연결 실패도 재시도 후 FAILED")
        void it_retries_connection_failure() {
            server.stop(0);

            assertThat(adapter.send(CHAT, List.of(T1))).isEqualTo(PushOutcome.FAILED);
        }

        @Test
        @DisplayName("ticket MessageRateExceeded 는 실패한 토큰만 다시 보낸다")
        void it_retries_only_rate_limited_tokens() {
            respond(200, "{\"data\":[{\"status\":\"ok\"}," + ticketError("MessageRateExceeded") + "]}");
            respond(200, ok(1));

            assertThat(adapter.send(CHAT, List.of(T1, T2))).isEqualTo(PushOutcome.SENT);
            assertThat(requests).hasSize(2);
            assertThat(sentTokens(1)).containsExactly(T2);
        }
    }

    @Nested
    @DisplayName("영구 실패는")
    class Describe_permanent {

        @Test
        @DisplayName("HTTP 400 을 재시도하지 않고 FAILED")
        void it_does_not_retry_4xx() {
            respond(400, "{\"errors\":[]}");

            assertThat(adapter.send(CHAT, List.of(T1))).isEqualTo(PushOutcome.FAILED);
            assertThat(requests).hasSize(1);
        }

        @Test
        @DisplayName("ticket DeviceNotRegistered 를 재시도하지 않고 FAILED (기존: 예약 유형은 청크 전체 3회 재전송)")
        void it_does_not_retry_device_not_registered() {
            respond(200, "{\"data\":[{\"status\":\"ok\"}," + ticketError("DeviceNotRegistered") + "]}");

            PushMessage reminder = new PushMessage("시민화폐, 광산", "본문", NotificationType.RESERVATION_REMINDER, 3L);
            assertThat(adapter.send(reminder, List.of(T1, T2))).isEqualTo(PushOutcome.FAILED);
            assertThat(requests).hasSize(1);
        }

        @Test
        @DisplayName("ticket 수가 요청과 다르면 토큰을 특정할 수 없으므로 재시도하지 않는다")
        void it_does_not_retry_misaligned_tickets() {
            respond(200, "{\"data\":[" + ticketError("MessageRateExceeded") + "]}");

            assertThat(adapter.send(CHAT, List.of(T1, T2))).isEqualTo(PushOutcome.FAILED);
            assertThat(requests).hasSize(1);
        }
    }

    @Nested
    @DisplayName("최종 실패 기록은")
    class Describe_final_failure_log {

        @Test
        @DisplayName("일시 실패 소진은 kind=TRANSIENT 로 남긴다")
        void it_logs_transient_final_failure(CapturedOutput output) {
            respond(500, "e");
            respond(500, "e");
            respond(500, "e");

            adapter.send(CHAT, List.of(T1));

            assertThat(output).contains("[Expo] 최종 실패: kind=TRANSIENT, type=CHATTING, sourceId=5");
        }

        @Test
        @DisplayName("HTTP 4xx 와 영구 ticket 오류는 kind=PERMANENT 로 남긴다")
        void it_logs_permanent_final_failure(CapturedOutput output) {
            respond(400, "{}");
            adapter.send(CHAT, List.of(T1));
            assertThat(output).contains("[Expo] 최종 실패: kind=PERMANENT, type=CHATTING, sourceId=5, tokens=1");

            respond(200, "{\"data\":[" + ticketError("DeviceNotRegistered") + "]}");
            PushMessage cancel = new PushMessage("t", "b", NotificationType.RESERVATION_CANCEL, 8L);
            adapter.send(cancel, List.of(T2));
            assertThat(output).contains("[Expo] 최종 실패: kind=PERMANENT, type=RESERVATION_CANCEL, sourceId=8, tokens=1");
        }

        @Test
        @DisplayName("성공과 일시 실패 후 성공은 최종 실패를 남기지 않는다")
        void it_does_not_log_on_success(CapturedOutput output) {
            respond(503, "e");
            respond(200, ok(1));

            assertThat(adapter.send(CHAT, List.of(T1))).isEqualTo(PushOutcome.SENT);
            assertThat(output).doesNotContain("최종 실패");
        }
    }

    @Test
    @DisplayName("운영 RetryTemplate(RetryConfig)은 기존 3회 시도·1초 고정 backoff 를 유지한다")
    void it_keeps_existing_retry_config() {
        RetryTemplate template = new RetryConfig().retryTemplate();
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        long start = System.nanoTime();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> template.execute(ctx -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("x");
        })).isInstanceOf(IllegalStateException.class);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(elapsedMs).as("1초 backoff 2회").isBetween(1900L, 5000L);
    }

    @Nested
    @DisplayName("해석할 수 없는 응답은")
    class Describe_unparsable {

        @Test
        @DisplayName("기존처럼 오류 없음으로 보고 SENT")
        void it_treats_malformed_as_ok() {
            respond(200, "not-json");
            assertThat(adapter.send(CHAT, List.of(T1))).isEqualTo(PushOutcome.SENT);
            respond(200, "{\"data\":{}}");
            assertThat(adapter.send(CHAT, List.of(T1))).isEqualTo(PushOutcome.SENT);
        }
    }

    @Nested
    @DisplayName("buildData() 는")
    class Describe_buildData {

        @Test
        @DisplayName("채팅·예약 유형에만 roomId 를 넣고 sourceId 가 없으면 alertType 만 넣는다")
        void it_builds_existing_data() {
            assertThat(adapter.buildData(NotificationType.CHATTING, 7L))
                    .isEqualTo(Map.of("alertType", "CHATTING", "sourceId", "7", "roomId", "7"));
            assertThat(adapter.buildData(NotificationType.RESERVATION_CANCEL, 7L)).containsEntry("roomId", "7");
            assertThat(adapter.buildData(NotificationType.TRADE_COMPLETE, 7L))
                    .isEqualTo(Map.of("alertType", "TRADE_COMPLETE", "sourceId", "7"));
            assertThat(adapter.buildData(NotificationType.NOTICE, null)).isEqualTo(Map.of("alertType", "NOTICE"));
        }
    }

    @Test
    @DisplayName("일시 실패 분류는 429 와 5xx 만 포함한다")
    void it_classifies_http_status() {
        assertThat(ExpoPushAdapter.isTransient(org.springframework.http.HttpStatusCode.valueOf(429))).isTrue();
        assertThat(ExpoPushAdapter.isTransient(org.springframework.http.HttpStatusCode.valueOf(502))).isTrue();
        assertThat(ExpoPushAdapter.isTransient(org.springframework.http.HttpStatusCode.valueOf(400))).isFalse();
        assertThat(ExpoPushAdapter.isTransient(org.springframework.http.HttpStatusCode.valueOf(401))).isFalse();
    }
}
