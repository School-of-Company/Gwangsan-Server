package team.startup.gwangsan.adapter.web.suspend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.application.suspend.data.SuspendMemberCommand;
import team.startup.gwangsan.application.suspend.port.in.SuspendMemberUseCase;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("정지 API 독립 HTTP 계약")
class SuspendControllerStandaloneTest {

    private MockMvc mvc;
    private SuspendMemberUseCase suspendMemberUseCase;

    @BeforeEach
    void setUp() {
        suspendMemberUseCase = mock(SuspendMemberUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new SuspendController(suspendMemberUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("it_요청 본문을 command 로 넘기고 204 를 반환한다")
    void it_returns_no_content() throws Exception {
        mvc.perform(patch("/api/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":2,\"suspendedDays\":7,\"alertId\":5}"))
                .andExpect(status().isNoContent());

        verify(suspendMemberUseCase).execute(new SuspendMemberCommand(2L, 7, 5L));
    }

    @Test
    @DisplayName("it_suspendedDays 누락은 기존처럼 0일 정지로 전달된다 (primitive @NotNull 기준선)")
    void it_keeps_primitive_days_default() throws Exception {
        mvc.perform(patch("/api/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":2}"))
                .andExpect(status().isNoContent());

        verify(suspendMemberUseCase).execute(new SuspendMemberCommand(2L, 0, null));
    }

    @Test
    @DisplayName("it_memberId 누락은 400 으로 거절하고 유스케이스를 실행하지 않는다")
    void it_rejects_missing_member_id() throws Exception {
        mvc.perform(patch("/api/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspendedDays\":7}"))
                .andExpect(status().isBadRequest());

        verify(suspendMemberUseCase, never()).execute(any());
    }
}
