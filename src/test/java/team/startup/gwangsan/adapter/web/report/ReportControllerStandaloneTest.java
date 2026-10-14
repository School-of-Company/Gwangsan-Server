package team.startup.gwangsan.adapter.web.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.adapter.web.FixedActorResolver;
import team.startup.gwangsan.application.report.data.CreateReportCommand;
import team.startup.gwangsan.application.report.port.in.CreateReportUseCase;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.model.ReportTargetType;
import team.startup.gwangsan.domain.report.model.ReportType;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("신고 API 독립 HTTP 계약")
class ReportControllerStandaloneTest {

    private MockMvc mvc;
    private CreateReportUseCase createReportUseCase;

    @BeforeEach
    void setUp() {
        createReportUseCase = mock(CreateReportUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportController(createReportUseCase))
                .setCustomArgumentResolvers(FixedActorResolver.user(1L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("신고 생성은")
    class CreateReport {
        @Test
        @DisplayName("it_인증 실행자와 targetType·imageIds 를 command 로 넘기고 201 빈 본문을 반환한다")
        void it_binds_target_type_enum_and_image_ids() throws Exception {
            mvc.perform(post("/api/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"PRODUCT\",\"sourceId\":7,\"reportType\":\"SPAM_AD\",\"content\":\"광고\",\"imageIds\":[3,4]}"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(createReportUseCase).execute(new CreateReportCommand(
                    1L, ReportTargetType.PRODUCT, 7L, ReportType.SPAM_AD, "광고", List.of(3L, 4L)));
        }

        @Test
        @DisplayName("it_targetType 이 없는 기존 요청은 회원 신고 command 로 넘긴다")
        void it_defaults_missing_target_type_to_member() throws Exception {
            mvc.perform(post("/api/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourceId\":7,\"reportType\":\"ETC\",\"content\":\"기타\"}"))
                    .andExpect(status().isCreated());

            verify(createReportUseCase).execute(new CreateReportCommand(
                    1L, ReportTargetType.MEMBER, 7L, ReportType.ETC, "기타", null));
        }

        @Test
        @DisplayName("it_알 수 없는 enum 은 유스케이스 실행 전에 400 으로 거절한다")
        void it_rejects_unknown_enum_before_service_execution() throws Exception {
            mvc.perform(post("/api/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"UNKNOWN\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(createReportUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("it_중복 신고는 409 로 변환한다")
        void it_converts_duplicate_report_to_conflict_response() throws Exception {
            doThrow(new AlreadyReportedException()).when(createReportUseCase).execute(any());

            mvc.perform(post("/api/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourceId\":7,\"reportType\":\"SPAM_AD\",\"content\":\"광고\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }
}
