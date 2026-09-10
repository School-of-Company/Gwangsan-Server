package team.startup.gwangsan.domain.chat.presentation.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomsResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = GetRoomsResponseJsonTest.JsonConfig.class)
class GetRoomsResponseJsonTest {

    @Configuration(proxyBeanMethods = false)
    static class JsonConfig {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("채팅 목록 응답 직렬화는")
    class Describe_serialization {

        @DisplayName("false인 상태 필드도 boolean 값으로 포함한다")
        @ParameterizedTest
        @CsvSource({"true, false", "false, true", "false, false"})
        void it_includes_boolean_status_fields_even_when_false(boolean completed, boolean reserved) throws Exception {
            var product = new GetRoomProductDto(1L, "상품", completed, reserved, List.of());
            var response = new GetRoomsResponse(1L, null, null, null, null, null, 0L, product);

            var json = objectMapper.readTree(objectMapper.writeValueAsString(List.of(response))).get(0).get("product");

            assertThat(json.has("isReserved")).isTrue();
            assertThat(json.get("isReserved").isBoolean()).isTrue();
            assertThat(json.get("isReserved").booleanValue()).isEqualTo(reserved);
            assertThat(json.get("isCompleted").isBoolean()).isTrue();
            assertThat(json.get("isCompleted").booleanValue()).isEqualTo(completed);
            assertThat(json.get("images").isArray()).isTrue();
            assertThat(json.get("images").isEmpty()).isTrue();
        }
    }
}
