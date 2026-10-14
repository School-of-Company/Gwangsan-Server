package team.startup.gwangsan.adapter.web.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team.startup.gwangsan.adapter.security.auth.ActorContextArgumentResolver;
import team.startup.gwangsan.adapter.security.auth.MemberDetails;
import team.startup.gwangsan.adapter.security.auth.MemberDetailsService;
import team.startup.gwangsan.adapter.security.auth.TokenParser;
import team.startup.gwangsan.adapter.web.alert.controller.AlertController;
import team.startup.gwangsan.application.alert.data.AlertImage;
import team.startup.gwangsan.application.alert.data.AlertView;
import team.startup.gwangsan.application.alert.port.in.MyAlertsQuery;
import team.startup.gwangsan.application.alert.port.in.ReadAlertsUseCase;
import team.startup.gwangsan.application.alert.port.in.UnreadAlertQuery;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.adapter.security.auth.JwtProvider;
import team.startup.gwangsan.infrastructure.config.WebMvcConfig;
import team.startup.gwangsan.infrastructure.security.JwtAccessDeniedHandler;
import team.startup.gwangsan.infrastructure.security.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.infrastructure.security.SecurityConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
@ContextConfiguration(classes = {AlertController.class, SecurityConfig.class, WebMvcConfig.class, ActorContextArgumentResolver.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost")
@DisplayName("알림 API 계약")
class AlertControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean MyAlertsQuery myAlertsQuery;
    @MockitoBean UnreadAlertQuery unreadAlertQuery;
    @MockitoBean ReadAlertsUseCase readAlertsUseCase;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean TokenParser tokenParser;
    @MockitoBean MemberDetailsService memberDetailsService;

    private static MemberDetails principal() {
        return new MemberDetails(new MemberAccount(7L, "nick", "010", "pw", MemberRole.ROLE_USER, MemberStatus.ACTIVE));
    }

    @Test
    @DisplayName("GET /api/alert 는 인증 회원의 알림을 기존 JSON 필드로 반환한다")
    void it_returns_alerts() throws Exception {
        when(myAlertsQuery.query(7L)).thenReturn(List.of(
                new AlertView(1L, "제목", "내용", AlertType.TRADE_CANCEL, LocalDateTime.of(2026, 1, 2, 3, 4, 5),
                        List.of(new AlertImage(11L, "https://img")), 3L, 99L),
                new AlertView(2L, "공지", "공지가 등록되었습니다.", AlertType.NOTICE, LocalDateTime.of(2026, 1, 1, 0, 0),
                        List.of(), null, 5L)));

        mvc.perform(get("/api/alert").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("제목"))
                .andExpect(jsonPath("$[0].content").value("내용"))
                .andExpect(jsonPath("$[0].alertType").value("TRADE_CANCEL"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-02T03:04:05"))
                .andExpect(jsonPath("$[0].images[0].imageId").value(11))
                .andExpect(jsonPath("$[0].images[0].imageUrl").value("https://img"))
                .andExpect(jsonPath("$[0].sendMemberId").value(3))
                .andExpect(jsonPath("$[0].sourceId").value(99))
                .andExpect(jsonPath("$[1].images").isEmpty())
                .andExpect(jsonPath("$[1].sendMemberId").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/alert/unread 는 unread 필드를 반환한다")
    void it_returns_unread() throws Exception {
        when(unreadAlertQuery.hasUnread(7L)).thenReturn(true);

        mvc.perform(get("/api/alert/unread").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(true));
    }

    @Test
    @DisplayName("PATCH /api/alert/read/{alert_id} 는 204 를 반환한다")
    void it_reads_alerts() throws Exception {
        mvc.perform(patch("/api/alert/read/15").with(user(principal())))
                .andExpect(status().isNoContent());

        verify(readAlertsUseCase).readUpTo(7L, 15L);
    }

    @Test
    @DisplayName("인증이 없으면 401 이고 유스케이스를 호출하지 않는다")
    void it_requires_authentication() throws Exception {
        mvc.perform(get("/api/alert")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/alert/unread")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/alert/read/1")).andExpect(status().isUnauthorized());

        verifyNoInteractions(myAlertsQuery, unreadAlertQuery, readAlertsUseCase);
    }
}
