package team.startup.gwangsan.domain.report.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.report.entity.constant.ReportTargetType;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateReportRequest;
import team.startup.gwangsan.domain.report.service.CreateReportService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("신고 API 독립 HTTP 계약")
class ReportControllerStandaloneTest {
    private MockMvc mvc;
    private CreateReportService createReportService;

    @BeforeEach
    void setUp() {
        createReportService = mock(CreateReportService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportController(createReportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("신고 생성은")
    class CreateReport {
        @Test
        void it_binds_target_type_enum_and_image_ids() throws Exception {
            mvc.perform(post("/api/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"PRODUCT\",\"sourceId\":7,\"reportType\":\"SPAM_AD\",\"content\":\"광고\",\"imageIds\":[3,4]}"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(createReportService).execute(new CreateReportRequest(
                    ReportTargetType.PRODUCT, 7L, ReportType.SPAM_AD, "광고", java.util.List.of(3L, 4L)));
        }

        @Test
        void it_rejects_unknown_enum_before_service_execution() throws Exception {
            mvc.perform(post("/api/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"UNKNOWN\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(createReportService, never()).execute(any());
        }

        @Test
        void it_converts_duplicate_report_to_conflict_response() throws Exception {
            doThrow(new AlreadyReportedException()).when(createReportService).execute(any());

            mvc.perform(post("/api/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourceId\":7,\"reportType\":\"SPAM_AD\",\"content\":\"광고\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }
}
