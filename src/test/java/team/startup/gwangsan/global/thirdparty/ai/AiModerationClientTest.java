package team.startup.gwangsan.global.thirdparty.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
