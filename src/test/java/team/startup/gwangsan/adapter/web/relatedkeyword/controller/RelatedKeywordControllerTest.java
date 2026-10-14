package team.startup.gwangsan.adapter.web.relatedkeyword.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.adapter.security.auth.ActorContextArgumentResolver;
import team.startup.gwangsan.adapter.security.auth.MemberDetails;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.relatedkeyword.data.DeleteMemberRelatedKeywordCommand;
import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteMemberRelatedKeywordUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.in.FindMemberRelatedKeywordsQuery;
import team.startup.gwangsan.application.relatedkeyword.port.in.FindRelatedKeywordsQuery;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordForbiddenException;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordNotFoundException;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("관심 키워드 API HTTP 계약")
class RelatedKeywordControllerTest {

    private final FindRelatedKeywordsQuery findAll = mock(FindRelatedKeywordsQuery.class);
    private final FindMemberRelatedKeywordsQuery findMine = mock(FindMemberRelatedKeywordsQuery.class);
    private final DeleteMemberRelatedKeywordUseCase delete = mock(DeleteMemberRelatedKeywordUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new RelatedKeywordController(findAll, findMine, delete))
                .setCustomArgumentResolvers(new ActorContextArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/related-keyword 는")
    class FindAll {
        @Test
        void it_returns_keywords_in_port_order_with_original_field_names() throws Exception {
            when(findAll.query()).thenReturn(List.of(new RelatedKeywordResult(2L, "요리"), new RelatedKeywordResult(1L, "청소")));

            mvc.perform(get("/api/related-keyword"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[{\"relatedKeywordId\":2,\"name\":\"요리\"},{\"relatedKeywordId\":1,\"name\":\"청소\"}]", true));
        }

        @Test
        void it_returns_empty_array() throws Exception {
            when(findAll.query()).thenReturn(List.of());

            mvc.perform(get("/api/related-keyword"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]", true));
        }
    }

    @Nested
    @DisplayName("GET /api/related-keyword/current 는")
    class FindMine {
        @Test
        void it_queries_with_authenticated_member_id() throws Exception {
            authenticate(7L);
            when(findMine.query(7L)).thenReturn(List.of(new RelatedKeywordResult(5L, "수리")));

            mvc.perform(get("/api/related-keyword/current?memberId=999"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[{\"relatedKeywordId\":5,\"name\":\"수리\"}]", true));
            verify(findMine).query(7L);
        }

        @Test
        void it_returns_401_without_member_principal() throws Exception {
            mvc.perform(get("/api/related-keyword/current"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
            verifyNoInteractions(findMine);
        }
    }

    @Nested
    @DisplayName("DELETE /api/related-keyword/{id} 는")
    class Delete {
        @Test
        void it_returns_no_content_with_actor_and_path_id() throws Exception {
            authenticate(7L);

            mvc.perform(delete("/api/related-keyword/3"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
            verify(delete).execute(new DeleteMemberRelatedKeywordCommand(7L, 3L));
        }

        @Test
        void it_maps_not_found_to_404() throws Exception {
            authenticate(7L);
            doThrow(new MemberRelatedKeywordNotFoundException()).when(delete).execute(any());

            mvc.perform(delete("/api/related-keyword/3"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당 관련 키워드를 찾을 수 없습니다."));
        }

        @Test
        void it_maps_other_members_keyword_to_403() throws Exception {
            authenticate(7L);
            doThrow(new MemberRelatedKeywordForbiddenException()).when(delete).execute(any());

            mvc.perform(delete("/api/related-keyword/3"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("해당 관련 키워드에 대한 권한이 없습니다."));
        }

        @Test
        void it_rejects_non_numeric_id_before_use_case() throws Exception {
            authenticate(7L);

            mvc.perform(delete("/api/related-keyword/abc"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(delete);
        }
    }

    private void authenticate(Long memberId) {
        MemberDetails details = new MemberDetails(new MemberAccount(
                memberId, "member", "010-1234-5678", "encoded", MemberRole.ROLE_USER, MemberStatus.ACTIVE));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
