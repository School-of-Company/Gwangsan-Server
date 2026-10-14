package team.startup.gwangsan.adapter.web.notice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import team.startup.gwangsan.application.notice.data.CreateNoticeCommand;
import team.startup.gwangsan.application.notice.data.DeleteNoticeCommand;
import team.startup.gwangsan.application.notice.data.NoticeDetailCriteria;
import team.startup.gwangsan.application.notice.data.NoticeDetailResult;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.application.notice.data.NoticeListCriteria;
import team.startup.gwangsan.application.notice.data.NoticeSummaryResult;
import team.startup.gwangsan.application.notice.data.UpdateNoticeCommand;
import team.startup.gwangsan.application.notice.port.in.CreateNoticeUseCase;
import team.startup.gwangsan.application.notice.port.in.DeleteNoticeUseCase;
import team.startup.gwangsan.application.notice.port.in.NoticeDetailQuery;
import team.startup.gwangsan.application.notice.port.in.NoticeListQuery;
import team.startup.gwangsan.application.notice.port.in.UpdateNoticeUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeForbiddenException;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.exception.UnauthorizedNoticeAccessException;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.time.LocalDateTime;
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

    private static final ActorContext ACTOR = new ActorContext(7L, ActorContext.Role.ROLE_HEAD_ADMIN);

    private MockMvc mvc;
    private CreateNoticeUseCase createNoticeUseCase;
    private NoticeListQuery noticeListQuery;
    private NoticeDetailQuery noticeDetailQuery;
    private UpdateNoticeUseCase updateNoticeUseCase;
    private DeleteNoticeUseCase deleteNoticeUseCase;

    @BeforeEach
    void setUp() {
        createNoticeUseCase = mock(CreateNoticeUseCase.class);
        noticeListQuery = mock(NoticeListQuery.class);
        noticeDetailQuery = mock(NoticeDetailQuery.class);
        updateNoticeUseCase = mock(UpdateNoticeUseCase.class);
        deleteNoticeUseCase = mock(DeleteNoticeUseCase.class);
        NoticeController controller = new NoticeController(
                createNoticeUseCase, noticeListQuery, noticeDetailQuery, updateNoticeUseCase, deleteNoticeUseCase);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new FixedActorResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 실제 앱에서는 ActorContextArgumentResolver 가 인증 principal 로 채운다.
    private static class FixedActorResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return ActorContext.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return ACTOR;
        }
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

            verify(createNoticeUseCase).execute(new CreateNoticeCommand(ACTOR, "점검 안내", "내용", 3, List.of(8L, 9L)));
        }

        @Test
        void it_rejects_missing_required_fields_before_service_execution() throws Exception {
            mvc.perform(post("/api/notice")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"내용\",\"placeId\":3}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(createNoticeUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("공지 조회는")
    class FindNotice {
        @Test
        void it_binds_cursor_and_size_and_serializes_notice_list() throws Exception {
            when(noticeListQuery.query(new NoticeListCriteria(ACTOR, 30L, 2))).thenReturn(List.of(new NoticeSummaryResult(
                    31L, "제목", "내용", List.of(new NoticeImageResult(4L, "cdn.example/4.png")), true)));

            mvc.perform(get("/api/notice").param("lastId", "30").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(31))
                    .andExpect(jsonPath("$[0].title").value("제목"))
                    .andExpect(jsonPath("$[0].content").value("내용"))
                    .andExpect(jsonPath("$[0].images[0].imageId").value(4))
                    .andExpect(jsonPath("$[0].images[0].imageUrl").value("cdn.example/4.png"))
                    .andExpect(jsonPath("$[0].isMe").value(true));
        }

        @Test
        void it_uses_first_page_and_default_size() throws Exception {
            when(noticeListQuery.query(new NoticeListCriteria(ACTOR, null, 10))).thenReturn(List.of());

            mvc.perform(get("/api/notice"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void it_converts_missing_notice_to_not_found_response() throws Exception {
            when(noticeDetailQuery.query(new NoticeDetailCriteria(ACTOR, 9L))).thenThrow(new NoticeNotFoundException());

            mvc.perform(get("/api/notice/9"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당 공지가 존재하지 않습니다."));
        }

        @Test
        void it_serializes_single_notice_from_path() throws Exception {
            when(noticeDetailQuery.query(new NoticeDetailCriteria(ACTOR, 9L))).thenReturn(new NoticeDetailResult(
                    9L, "제목", "내용", "광산구", LocalDateTime.of(2026, 1, 2, 3, 4, 5), "ROLE_HEAD_ADMIN",
                    List.of(new NoticeImageResult(4L, "cdn.example/4.png")), false));

            mvc.perform(get("/api/notice/9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.place").value("광산구"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.role").value("ROLE_HEAD_ADMIN"))
                    .andExpect(jsonPath("$.images[0].imageId").value(4))
                    .andExpect(jsonPath("$.isMe").value(false));
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

            verify(updateNoticeUseCase).execute(new UpdateNoticeCommand(ACTOR, 5L, "수정", "새 내용", List.of(7L)));
        }

        @Test
        void it_converts_non_author_update_to_forbidden_response() throws Exception {
            doThrow(new UnauthorizedNoticeAccessException()).when(updateNoticeUseCase).execute(any());

            mvc.perform(patch("/api/notice/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"수정\",\"content\":\"새 내용\",\"imageIds\":[]}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("공지 작성자만 수정할 수 있습니다."));
        }

        @Test
        void it_deletes_with_no_content() throws Exception {
            mvc.perform(delete("/api/notice/5"))
                    .andExpect(status().isNoContent());

            verify(deleteNoticeUseCase).execute(new DeleteNoticeCommand(ACTOR, 5L));
        }

        @Test
        void it_converts_forbidden_delete_to_forbidden_response() throws Exception {
            doThrow(new NoticeForbiddenException()).when(deleteNoticeUseCase).execute(any());

            mvc.perform(delete("/api/notice/5"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void it_converts_missing_deleted_notice_to_not_found_response() throws Exception {
            doThrow(new NoticeNotFoundException()).when(deleteNoticeUseCase).execute(any());

            mvc.perform(delete("/api/notice/5"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
