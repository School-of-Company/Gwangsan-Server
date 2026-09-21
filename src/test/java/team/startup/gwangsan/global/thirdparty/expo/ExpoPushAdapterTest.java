package team.startup.gwangsan.global.thirdparty.expo;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpoPushAdapter 단위 테스트")
class ExpoPushAdapterTest {

    private ExpoPushAdapter adapter;

    @Mock
    private org.springframework.retry.support.RetryTemplate retryTemplate;

    @Mock(answer = Answers.RETURNS_SELF)
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient expoClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(expoClient);
        adapter = new ExpoPushAdapter(retryTemplate, webClientBuilder, new Gson());
    }

    @Nested
    @DisplayName("isExpoPushToken() 메서드는")
    class Describe_isExpoPushToken {

        @Test
        @DisplayName("ExponentPushToken 형식이면 true를 반환한다")
        void it_returns_true_for_expo_token() {
            assertThat(adapter.isExpoPushToken("ExponentPushToken[abc123]")).isTrue();
        }

        @Test
        @DisplayName("네이티브 FCM 토큰이면 false를 반환한다")
        void it_returns_false_for_native_fcm_token() {
            assertThat(adapter.isExpoPushToken("fcm-native-registration-token")).isFalse();
        }

        @Test
        @DisplayName("null이면 false를 반환한다")
        void it_returns_false_for_null() {
            assertThat(adapter.isExpoPushToken(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("buildData() 메서드는")
    class Describe_buildData {

        @Test
        @DisplayName("CHATTING 타입일 때 alertType, sourceId, roomId를 모두 포함한다")
        void it_includes_room_id_for_chatting() {
            Map<String, String> data = adapter.buildData(NotificationType.CHATTING, 42L);

            assertThat(data)
                    .containsEntry("alertType", "CHATTING")
                    .containsEntry("sourceId", "42")
                    .containsEntry("roomId", "42");
        }

        @Test
        @DisplayName("TRADE_COMPLETE 타입일 때 alertType, sourceId만 포함하고 roomId는 없다")
        void it_excludes_room_id_for_trade_complete() {
            Map<String, String> data = adapter.buildData(NotificationType.TRADE_COMPLETE, 7L);

            assertThat(data)
                    .containsEntry("alertType", "TRADE_COMPLETE")
                    .containsEntry("sourceId", "7")
                    .doesNotContainKey("roomId");
        }

        @Test
        @DisplayName("sourceId가 null이면 sourceId, roomId 없이 alertType만 포함한다")
        void it_omits_source_id_and_room_id_when_source_id_is_null() {
            Map<String, String> data = adapter.buildData(NotificationType.CHATTING, null);

            assertThat(data)
                    .containsEntry("alertType", "CHATTING")
                    .doesNotContainKey("sourceId")
                    .doesNotContainKey("roomId");
        }
    }

    @Nested
    @DisplayName("sendNotification() 메서드는")
    class Describe_sendNotification {

        @Test
        @DisplayName("type이 null이면 전송을 시도하지 않는다")
        void it_does_not_attempt_to_send_when_type_is_null() {
            List<DeviceToken> tokens = List.of(mock(DeviceToken.class));

            adapter.sendNotification(tokens, "title", "body", null, 1L);

            verifyNoInteractions(retryTemplate);
        }

        @Test
        @DisplayName("Expo 형식이 아닌 토큰만 있으면 전송을 시도하지 않는다")
        void it_does_not_attempt_to_send_when_no_token_is_expo_format() {
            DeviceToken nativeToken = DeviceToken.builder()
                    .deviceId("device-1")
                    .userId(1L)
                    .deviceToken("fcm-native-registration-token")
                    .build();

            adapter.sendNotification(List.of(nativeToken), "title", "body", NotificationType.CHATTING, 1L);

            verifyNoInteractions(retryTemplate);
        }

        @Test
        @DisplayName("수신자 목록이 null 또는 비어 있으면 전송을 시도하지 않는다")
        void it_skips_null_and_empty_recipient_lists() {
            adapter.sendNotification(null, "title", "body", NotificationType.CHATTING, 1L);
            adapter.sendNotification(List.of(), "title", "body", NotificationType.CHATTING, 1L);

            verifyNoInteractions(retryTemplate);
        }

        @Test
        @DisplayName("101개의 Expo 토큰은 100개와 1개 청크로 전송한다")
        void it_sends_tokens_in_100_item_chunks() {
            when(expoClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri("/--/api/v2/push/send")).thenReturn(requestBodySpec);
            doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"data\":[]}"));

            ExpoPushAdapter sendingAdapter = new ExpoPushAdapter(
                    org.springframework.retry.support.RetryTemplate.builder().maxAttempts(1).build(),
                    webClientBuilder,
                    new Gson()
            );
            List<DeviceToken> tokens = IntStream.range(0, 101)
                    .mapToObj(index -> DeviceToken.builder()
                            .deviceToken("ExponentPushToken[" + index + "]")
                            .build())
                    .toList();

            sendingAdapter.sendNotification(tokens, "title", "body", NotificationType.CHATTING, 7L);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(expoClient, times(2)).post();
            verify(requestBodyUriSpec, times(2)).uri("/--/api/v2/push/send");
            verify(requestBodySpec, times(2)).bodyValue(payloadCaptor.capture());
            verify(responseSpec, times(2)).bodyToMono(String.class);
            assertThat(payloadCaptor.getAllValues())
                    .extracting(payload -> ((List<?>) payload).size())
                    .containsExactly(100, 1);
        }
    }

    @Nested
    @DisplayName("expoTokens() 메서드는")
    class Describe_expoTokens {

        @Test
        @DisplayName("Expo 토큰만 중복 없이 반환한다")
        void it_returns_distinct_expo_tokens() {
            List<DeviceToken> tokens = Arrays.asList(
                    DeviceToken.builder().deviceToken("ExponentPushToken[abc]").build(),
                    DeviceToken.builder().deviceToken("ExponentPushToken[abc]").build(),
                    DeviceToken.builder().deviceToken("fcm-token").build(),
                    DeviceToken.builder().deviceToken(null).build(),
                    null
            );

            assertThat(adapter.expoTokens(tokens)).containsExactly("ExponentPushToken[abc]");
        }
    }

    @Nested
    @DisplayName("logExpoErrors() 메서드는")
    class Describe_logExpoErrors {

        @Test
        @DisplayName("Expo error ticket 개수를 반환한다")
        void it_returns_error_ticket_count() {
            String response = """
                    {"data":[
                      {"status":"ok","id":"ticket-id"},
                      {"status":"error","message":"DeviceNotRegistered","details":{"error":"DeviceNotRegistered"}}
                    ]}
                    """;

            assertThat(adapter.logExpoErrors(response)).isEqualTo(1);
        }

        @Test
        @DisplayName("빈 응답과 data 배열이 아닌 응답은 오류 없음으로 처리한다")
        void it_returns_zero_for_empty_or_non_ticket_responses() {
            assertThat(adapter.logExpoErrors(" ")).isZero();
            assertThat(adapter.logExpoErrors("null")).isZero();
            assertThat(adapter.logExpoErrors("{}")).isZero();
            assertThat(adapter.logExpoErrors("{\"data\":{}}")).isZero();
            assertThat(adapter.logExpoErrors("""
                    {"data":[null,"not-a-ticket",{"status":"ok"},{"message":"missing-status"}]}
                    """)).isZero();
        }

        @Test
        @DisplayName("오류 티켓의 선택 항목이 null 또는 누락되어도 오류 수를 센다")
        void it_counts_error_tickets_with_missing_optional_fields() {
            assertThat(adapter.logExpoErrors("""
                    {"data":[
                      {"status":"error","message":null,"details":{"error":null}},
                      {"status":"error"}
                    ]}
                    """)).isEqualTo(2);
        }

        @Test
        @DisplayName("잘못된 JSON 응답은 파싱 실패로 처리하고 0을 반환한다")
        void it_returns_zero_for_malformed_response() {
            assertThat(adapter.logExpoErrors("{not-json")).isZero();
        }
    }
}
