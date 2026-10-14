package team.startup.gwangsan.adapter.web.health.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 인증 필터 제외·실제 기동 경로는 JwtAuthenticationFilterTest·ApplicationContextStartupIntegrationTest 가 확인한다. */
@DisplayName("health HTTP 계약")
class HealthBoundaryTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();

    @Test
    @DisplayName("it_GET /api/health 는 200 text/plain \"ok\" 를 반환한다")
    void it_returns_ok_text() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("ok"));
    }
}
