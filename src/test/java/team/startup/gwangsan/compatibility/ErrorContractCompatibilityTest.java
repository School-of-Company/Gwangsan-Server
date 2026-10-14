package team.startup.gwangsan.compatibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.ErrorResponse;
import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 프론트가 의존하는 오류 계약을 고정한다: 모든 ErrorCode 의 이름·HTTP status·메시지와 응답 JSON 모양.
 * PR12 가 ErrorCode/GlobalException 을 domain/common 으로, 핸들러를 infrastructure/web 으로 옮길 때
 * import 만 바꾸고 스냅샷과 JSON 기대값은 그대로 통과해야 한다.
 */
@DisplayName("오류 응답 계약 호환")
class ErrorContractCompatibilityTest {

    private static final String SNAPSHOT = "compatibility/error-codes.snapshot";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("ErrorCode 카탈로그는")
    class Describe_catalog {

        @Test
        @DisplayName("it_기준 이름·status·메시지 스냅샷과 같다")
        void it_matches_snapshot() {
            assertThat(ErrorCode.values()).hasSizeGreaterThanOrEqualTo(86);
            String actual = Arrays.stream(ErrorCode.values())
                    .sorted(Comparator.comparing(Enum::name))
                    .map(code -> code.name() + "\t" + code.getStatus() + "\t" + code.getMessage())
                    .collect(Collectors.joining("\n", "", "\n"));

            Snapshots.assertMatches(SNAPSHOT, actual);
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler 는")
    class Describe_handler {

        @Test
        @DisplayName("it_업무 예외를 ErrorCode 의 status 와 {status,message} JSON 으로 응답한다")
        void it_maps_business_exception() throws Exception {
            ResponseEntity<ErrorResponse> response = handler.handle(new GlobalException(ErrorCode.NOT_FOUND_MEMBER));

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(objectMapper.writeValueAsString(response.getBody()))
                    .isEqualTo("{\"status\":404,\"message\":\"해당 회원을 찾을 수 없습니다.\"}");
        }

        @Test
        @DisplayName("it_요청 형식 오류를 400 고정 메시지로 응답한다")
        void it_maps_bad_request() throws Exception {
            ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                    new HttpMessageNotReadableException("broken", mock(HttpInputMessage.class)));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(objectMapper.writeValueAsString(response.getBody()))
                    .isEqualTo("{\"status\":400,\"message\":\"잘못된 요청입니다.\"}");
        }

        @Test
        @DisplayName("it_처리되지 않은 예외를 500 과 예외 메시지로 응답한다 (기준선 동작)")
        void it_maps_unhandled() throws Exception {
            ResponseEntity<ErrorResponse> response = handler.handleException(new IllegalStateException("boom"));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(objectMapper.writeValueAsString(response.getBody()))
                    .isEqualTo("{\"status\":500,\"message\":\"boom\"}");
        }
    }
}
