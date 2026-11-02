package team.startup.gwangsan.infrastructure.observability;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.adapter.event.chat.ChatMessageMutationEventListener;
import team.startup.gwangsan.adapter.http.chat.ChattingServerMessageMutationNotifier;
import team.startup.gwangsan.adapter.http.chat.ChattingServerProperties;
import team.startup.gwangsan.application.chat.data.ChatMessageMutationEvent;
import team.startup.gwangsan.infrastructure.config.AsyncConfig;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenTelemetry Java agent 를 붙인 JVM(./gradlew agentTest)에서만 돈다. 앱은 wrapper 없이
 * agent 의 executor·HTTP client 계측에 기대므로, 실제 agent 로 AFTER_COMMIT·@Async·RestClient 전파를 확인한다.
 */
@Tag("otel-agent")
@DisplayName("OpenTelemetry Java agent 전파")
class OpenTelemetryAgentPropagationTest {

    private HttpServer server;
    private final BlockingQueue<String> traceparents = new ArrayBlockingQueue<>(4);

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String header = exchange.getRequestHeaders().getFirst("traceparent");
            traceparents.add(header == null ? "missing" : header);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Nested
    @DisplayName("agent 가 붙으면")
    class Describe_agent {

        @Test
        @DisplayName("it_앱의 OTel API 가 agent SDK 에 연결돼 유효한 span 을 만든다")
        void it_bridges_application_api() {
            Span span = GlobalOpenTelemetry.getTracer("test").spanBuilder("bridge").startSpan();
            try {
                assertThat(span.getSpanContext().isValid()).isTrue();
            } finally {
                span.end();
            }
        }

        @Test
        @DisplayName("it_AFTER_COMMIT @Async listener 의 채팅 서버 호출에 요청 trace 를 traceparent 로 싣는다")
        void it_propagates_through_async_after_commit_notifier() throws Exception {
            int port = server.getAddress().getPort();
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                context.registerBean(ChattingServerProperties.class, () -> new ChattingServerProperties(
                        "http://127.0.0.1:" + port, "fictional-secret", Duration.ofSeconds(1), Duration.ofSeconds(2)));
                context.register(Wiring.class);
                context.refresh();

                Span request = GlobalOpenTelemetry.getTracer("test").spanBuilder("http request").startSpan();
                try (Scope ignored = request.makeCurrent()) {
                    context.getBean(TransactionTemplate.class).executeWithoutResult(status ->
                            context.publishEvent(new ChatMessageMutationEvent(ChatMessageMutationEvent.Kind.DELETED,
                                    1L, 2L, null, null, false, null)));
                } finally {
                    request.end();
                }

                String traceparent = traceparents.poll(10, TimeUnit.SECONDS);
                assertThat(traceparent).isNotNull()
                        .startsWith("00-" + request.getSpanContext().getTraceId() + "-");
            }
        }
    }

    @Configuration
    @EnableTransactionManagement
    @Import({AsyncConfig.class, ChatMessageMutationEventListener.class, ChattingServerMessageMutationNotifier.class})
    static class Wiring {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }
    }

    // AFTER_COMMIT 동기화만 필요해서 자원 없는 transaction manager 를 쓴다.
    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
