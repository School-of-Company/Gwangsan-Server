package team.startup.gwangsan.domain.notice.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.presentation.dto.request.CreateNoticeRequest;
import team.startup.gwangsan.domain.notice.presentation.dto.request.UpdateNoticeRequest;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindAllNoticeResponse;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindNoticeResponse;
import team.startup.gwangsan.domain.notice.service.CreateNoticeService;
import team.startup.gwangsan.domain.notice.service.DeleteNoticeService;
import team.startup.gwangsan.domain.notice.service.FindAllNoticeService;
import team.startup.gwangsan.domain.notice.service.FindNoticeService;
import team.startup.gwangsan.domain.notice.service.UpdateNoticeService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("공지 API 독립 HTTP 계약")
class NoticeControllerStandaloneTest {
    private MockMvc mvc;
    private CreateNoticeService createNoticeService;
    private FindAllNoticeService findAllNoticeService;
    private FindNoticeService findNoticeService;
    private UpdateNoticeService updateNoticeService;
    private DeleteNoticeService deleteNoticeService;

    @BeforeEach
    void setUp() {
        createNoticeService = mock(CreateNoticeService.class);
        findAllNoticeService = mock(FindAllNoticeService.class);
        findNoticeService = mock(FindNoticeService.class);
        updateNoticeService = mock(UpdateNoticeService.class);
        deleteNoticeService = mock(DeleteNoticeService.class);
        NoticeController controller = new NoticeController(
                createNoticeService,
                findAllNoticeService,
                findNoticeService,
                updateNoticeService,
                deleteNoticeService
        );
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("공지 생성은")
    class CreateNotice {
        @Test
        void it_binds_body_and_returns_created_without_body() throws Exception {
            mvc.perform(post("/api/notice")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"점검 안내\",\"content\":\"내용\",\"placeId\":3,\"imageIds\":[8,9]}"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(createNoticeService).execute(new CreateNoticeRequest("점검 안내", "내용", 3, java.util.List.of(8L, 9L)));
        }

        @Test
        void it_rejects_missing_required_fields_before_service_execution() throws Exception {
            mvc.perform(post("/api/notice")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"내용\",\"placeId\":3}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(createNoticeService, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("공지 조회는")
    class FindNotice {
        @Test
        void it_binds_cursor_and_size_and_serializes_notice_list() throws Exception {
            when(findAllNoticeService.execute(30L, 2)).thenReturn(List.of(new FindAllNoticeResponse(
                    31L, "제목", "내용", List.of(new GetImageResponse(4L, "cdn.example/4.png")), true
            )));

            mvc.perform(get("/api/notice").param("lastId", "30").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(31))
                    .andExpect(jsonPath("$[0].images[0].imageUrl").value("cdn.example/4.png"))
                    .andExpect(jsonPath("$[0].isMe").value(true));

            verify(findAllNoticeService).execute(30L, 2);
        }

        @Test
        void it_converts_missing_notice_to_not_found_response() throws Exception {
            when(findNoticeService.execute(9L)).thenThrow(new NoticeNotFoundException());

            mvc.perform(get("/api/notice/9"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        void it_serializes_single_notice_from_path() throws Exception {
            when(findNoticeService.execute(9L)).thenReturn(new FindNoticeResponse(
                    9L, "제목", "내용", "광산구", null, "HEAD_ADMIN", List.of(), false
            ));

            mvc.perform(get("/api/notice/9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.place").value("광산구"))
                    .andExpect(jsonPath("$.isMe").value(false));

            verify(findNoticeService).execute(9L);
        }
    }

    @Nested
    @DisplayName("공지 수정과 삭제는")
    class ChangeNotice {
        @Test
        void it_binds_update_path_and_body() throws Exception {
            mvc.perform(patch("/api/notice/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"수정\",\"content\":\"새 내용\",\"imageIds\":[7]}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(updateNoticeService).execute(5L, new UpdateNoticeRequest("수정", "새 내용", List.of(7L)));
        }

        @Test
        void it_converts_missing_deleted_notice_to_not_found_response() throws Exception {
            doThrow(new NoticeNotFoundException()).when(deleteNoticeService).execute(5L);

            mvc.perform(delete("/api/notice/5"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
