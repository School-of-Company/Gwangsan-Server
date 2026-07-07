package team.startup.gwangsan.global.thirdparty.expo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpoPushAdapter 단위 테스트")
class ExpoPushAdapterTest {

    @InjectMocks
    private ExpoPushAdapter adapter;

    @Mock
    private org.springframework.retry.support.RetryTemplate retryTemplate;

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
}
