package team.startup.gwangsan.global.thirdparty.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("AiModerationClient 단위 테스트")
class AiModerationClientTest {

    private MockRestServiceServer server;
    private AiModerationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiModerationClient(
                new AiModerationProperties(
                        "http://ai-server",
                        null,
                        null
                ),
                builder
        );
    }

    @Test
    @DisplayName("비속어 모델의 label 1 응답을 차단 대상으로 반환한다")
    void it_returns_true_for_profanity_label() {
        server.expect(once(), requestTo("http://ai-server/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"text":"부적절한 문장"}
                        """))
                .andRespond(withSuccess("""
                        {"label":1}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.containsProfanity("부적절한 문장")).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("NSFW 모델의 is_nsfw 응답을 매핑한다")
    void it_maps_snake_case_nsfw_response() {
        server.expect(once(), requestTo("http://ai-server/nsfw"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"is_nsfw":true}
                        """, MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        assertThat(client.isNsfw(file)).isTrue();
        server.verify();
    }

    @Nested
    @DisplayName("containsProfanity() 메서드는")
    class Describe_containsProfanity {

        @Test
        @DisplayName("label이 1이 아니면 차단 대상으로 반환하지 않는다")
        void it_returns_false_for_non_profanity_label() {
            server.expect(once(), requestTo("http://ai-server/predict"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"label":0}
                            """, MediaType.APPLICATION_JSON));

            assertThat(client.containsProfanity("정상 문장")).isFalse();
            server.verify();
        }

        @Test
        @DisplayName("빈 입력과 비활성화된 설정에서는 원격 요청 없이 false를 반환한다")
        void it_skips_blank_input_and_disabled_client() {
            AiModerationClient disabledClient = new AiModerationClient(
                    new AiModerationProperties("  ", null, null),
                    RestClient.builder()
            );

            assertThat(client.containsProfanity(null)).isFalse();
            assertThat(client.containsProfanity(" \t")).isFalse();
            assertThat(disabledClient.containsProfanity("검사할 문장")).isFalse();
            server.verify();
        }

        @Test
        @DisplayName("AI 서버 오류는 검사를 건너뛰고 false를 반환한다")
        void it_returns_false_when_predict_server_returns_error() {
            server.expect(once(), requestTo("http://ai-server/predict"))
                    .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

            assertThat(client.containsProfanity("검사할 문장")).isFalse();
            server.verify();
        }
    }

    @Nested
    @DisplayName("isNsfw() 메서드는")
    class Describe_isNsfw {

        @Test
        @DisplayName("false 응답은 false로 매핑한다")
        void it_returns_false_for_non_nsfw_response() {
            server.expect(once(), requestTo("http://ai-server/nsfw"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"is_nsfw":false}
                            """, MediaType.APPLICATION_JSON));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "safe.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
            );

            assertThat(client.isNsfw(file)).isFalse();
            server.verify();
        }

        @Test
        @DisplayName("AI 서버 오류는 이미지를 안전한 것으로 처리한다")
        void it_returns_false_when_nsfw_server_returns_error() {
            server.expect(once(), requestTo("http://ai-server/nsfw"))
                    .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "unavailable.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
            );

            assertThat(client.isNsfw(file)).isFalse();
            server.verify();
        }
    }
}
