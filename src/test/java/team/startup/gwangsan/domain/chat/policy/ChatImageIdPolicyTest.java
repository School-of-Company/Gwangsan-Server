package team.startup.gwangsan.domain.chat.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import team.startup.gwangsan.domain.chat.exception.InvalidChatImageIdsException;
import team.startup.gwangsan.global.exception.ErrorCode;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatImageIdPolicy 단위 테스트")
class ChatImageIdPolicyTest {

    @Nested
    @DisplayName("parse() 는")
    class Describe_parse {

        @Test
        @DisplayName("it_요청 순서와 중복을 유지하고 경계값·선행 0 을 허용한다")
        void it_keeps_order_and_accepts_boundaries() {
            assertThat(ChatImageIdPolicy.parse(List.of("11", "1", "11", "9007199254740991", "00011")))
                    .containsExactly(11L, 1L, 11L, 9_007_199_254_740_991L, 11L);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "0", "-1", "1.5", "abc", "9007199254740992", "9223372036854775808",
                "+11", "11 ", " 11", "11,12", "0xB", "1e1", "１１"})
        @DisplayName("it_양의 안전 정수가 아니면 INVALID_IMAGE_IDS(400) 를 던진다")
        void it_rejects_invalid_ids(String value) {
            assertThatThrownBy(() -> ChatImageIdPolicy.parse(List.of(value)))
                    .isInstanceOf(InvalidChatImageIdsException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_IMAGE_IDS);
        }

        @Test
        @DisplayName("it_빈 목록과 null 요소를 거부한다")
        void it_rejects_empty_and_null() {
            assertThatThrownBy(() -> ChatImageIdPolicy.parse(List.of())).isInstanceOf(InvalidChatImageIdsException.class);
            assertThatThrownBy(() -> ChatImageIdPolicy.parse(Arrays.asList("1", null)))
                    .isInstanceOf(InvalidChatImageIdsException.class);
        }
    }
}
