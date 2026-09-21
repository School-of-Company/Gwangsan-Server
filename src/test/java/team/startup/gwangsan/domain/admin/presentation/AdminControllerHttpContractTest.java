package team.startup.gwangsan.domain.admin.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.SignInAdminResponse;
import team.startup.gwangsan.domain.admin.service.*;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("관리자 HTTP 계약")
class AdminControllerHttpContractTest {
    private MockMvc mvc;
    private FindAlertByAlertTypeAndPlaceService findAlerts;
    private UpdateMemberRoleService updateMemberRole;
    private UpdateMemberStatusService updateMemberStatus;
    private AdjustGwangsanService adjustGwangsan;
    private SignInAdminService signIn;
    private RejectAdminAlertService rejectAlert;
    private VerificationSignUpService verifySignUp;
    private DeleteAdminAlertService deleteAlert;
    private ApproveTradeCancelService approveTradeCancel;

    @BeforeEach
    void setUp() {
        findAlerts = mock(FindAlertByAlertTypeAndPlaceService.class);
        updateMemberRole = mock(UpdateMemberRoleService.class);
        signIn = mock(SignInAdminService.class);
        updateMemberStatus = mock(UpdateMemberStatusService.class);
        rejectAlert = mock(RejectAdminAlertService.class);
        verifySignUp = mock(VerificationSignUpService.class);
        deleteAlert = mock(DeleteAdminAlertService.class);
        approveTradeCancel = mock(ApproveTradeCancelService.class);
        adjustGwangsan = mock(AdjustGwangsanService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(findAlerts, updateMemberRole, signIn,
                        updateMemberStatus, rejectAlert, verifySignUp, deleteAlert, approveTradeCancel, adjustGwangsan))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("알림 조회는")
    class AlertFilters {
        @Test
        void it_passes_absent_filters_as_null_and_serializes_empty_groups() throws Exception {
            when(findAlerts.execute(null, null)).thenReturn(new GetAdminAlertResponse(List.of(), List.of(), List.of()));

            mvc.perform(get("/api/admin/alert"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reports").isArray())
                    .andExpect(jsonPath("$.signUps").isArray())
                    .andExpect(jsonPath("$.tradeCancels").isArray());

            verify(findAlerts).execute(null, null);
        }

        @Test
        void it_binds_alert_type_and_place_filter() throws Exception {
            when(findAlerts.execute(7, AlertType.SIGN_UP))
                    .thenReturn(new GetAdminAlertResponse(List.of(), List.of(), List.of()));

            mvc.perform(get("/api/admin/alert").param("alert_type", "SIGN_UP").param("place_id", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.signUps").isArray());
            verify(findAlerts).execute(7, AlertType.SIGN_UP);
        }

        @Test
        void it_rejects_unknown_alert_type_as_bad_request() throws Exception {
            mvc.perform(get("/api/admin/alert").param("alert_type", "UNKNOWN"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(findAlerts);
        }
    }

    @Nested
    @DisplayName("관리자 변경 요청은")
    class Mutations {
        @Test
        void it_binds_member_role_and_returns_no_content() throws Exception {
            mvc.perform(patch("/api/admin/role/11").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ROLE_PLACE_ADMIN\",\"placeId\":3}"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(updateMemberRole).execute(11L, MemberRole.ROLE_PLACE_ADMIN, 3);
        }

        @Test
        void it_binds_member_status_and_returns_no_content() throws Exception {
            mvc.perform(patch("/api/admin/status/12").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"SUSPENDED\"}"))
                    .andExpect(status().isNoContent());

            verify(updateMemberStatus).execute(12L, MemberStatus.SUSPENDED);
        }

        @Test
        void it_binds_gwangsan_adjustment_and_returns_empty_ok_response() throws Exception {
            mvc.perform(patch("/api/admin/gwangsan/13").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"gwangsan\":5000}"))
                    .andExpect(status().isOk()).andExpect(content().string(""));

            verify(adjustGwangsan).execute(13L, 5000);
        }

        @Test
        void it_routes_alert_rejection_to_no_content() throws Exception {
            mvc.perform(delete("/api/admin/alert/14")).andExpect(status().isNoContent()).andExpect(content().string(""));
            verify(rejectAlert).execute(14L);
        }

        @Test
        void it_routes_signup_verification_to_no_content() throws Exception {
            mvc.perform(patch("/api/admin/verify/signup/15")).andExpect(status().isNoContent()).andExpect(content().string(""));
            verify(verifySignUp).execute(15L);
        }

        @Test
        void it_routes_alert_deletion_to_no_content() throws Exception {
            mvc.perform(delete("/api/admin/16")).andExpect(status().isNoContent()).andExpect(content().string(""));
            verify(deleteAlert).execute(16L);
        }

        @Test
        void it_routes_trade_approval_to_empty_ok_response() throws Exception {
            mvc.perform(patch("/api/admin/trade/17")).andExpect(status().isOk()).andExpect(content().string(""));
            verify(approveTradeCancel).execute(17L);
        }

        @Test
        void it_rejects_missing_required_role_before_service_call() throws Exception {
            mvc.perform(patch("/api/admin/role/11").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(updateMemberRole);
        }
    }

    @Nested
    @DisplayName("관리자 로그인과 도메인 오류는")
    class Responses {
        @Test
        void it_serializes_signin_response() throws Exception {
            when(signIn.execute("admin", "password"))
                    .thenReturn(new SignInAdminResponse(new TokenResponse("access", "refresh", null, null), MemberRole.ROLE_HEAD_ADMIN));

            mvc.perform(post("/api/admin/signin").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"admin\",\"password\":\"password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token.accessToken").value("access"))
                    .andExpect(jsonPath("$.role").value("ROLE_HEAD_ADMIN"));

            verify(signIn).execute("admin", "password");
        }

        @Test
        void it_maps_rejected_alert_not_found_to_error_json() throws Exception {
            doThrow(new NotFoundAdminAlertException()).when(rejectAlert).execute(99L);

            mvc.perform(delete("/api/admin/alert/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("존재하지 않는 어드민 알림입니다."));
        }

        @Test
        void it_maps_trade_approval_not_found_to_error_json() throws Exception {
            doThrow(new NotFoundAdminAlertException()).when(approveTradeCancel).execute(99L);

            mvc.perform(patch("/api/admin/trade/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("존재하지 않는 어드민 알림입니다."));
        }
    }
}
