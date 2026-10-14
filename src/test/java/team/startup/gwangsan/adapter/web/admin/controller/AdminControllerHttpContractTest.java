package team.startup.gwangsan.adapter.web.admin.controller;

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
import team.startup.gwangsan.application.admin.data.*;
import team.startup.gwangsan.application.admin.port.in.*;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("관리자 HTTP 계약")
class AdminControllerHttpContractTest {
    private static final ActorContext ADMIN = new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN);

    private MockMvc mvc;
    private FindAdminAlertsQuery findAlerts;
    private UpdateMemberRoleUseCase updateMemberRole;
    private UpdateMemberStatusUseCase updateMemberStatus;
    private AdjustGwangsanUseCase adjustGwangsan;
    private SignInAdminUseCase signIn;
    private RejectAdminAlertUseCase rejectAlert;
    private VerifySignUpUseCase verifySignUp;
    private DeleteAdminAlertUseCase deleteAlert;
    private ApproveTradeCancelUseCase approveTradeCancel;

    /** 인증 principal 대신 고정 실행자를 넣는다. 실제 resolver 는 ActorContextArgumentResolverTest 가 검증한다. */
    static final class FixedActorResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return ActorContext.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return ADMIN;
        }
    }

    @BeforeEach
    void setUp() {
        findAlerts = mock(FindAdminAlertsQuery.class);
        updateMemberRole = mock(UpdateMemberRoleUseCase.class);
        signIn = mock(SignInAdminUseCase.class);
        updateMemberStatus = mock(UpdateMemberStatusUseCase.class);
        rejectAlert = mock(RejectAdminAlertUseCase.class);
        verifySignUp = mock(VerifySignUpUseCase.class);
        deleteAlert = mock(DeleteAdminAlertUseCase.class);
        approveTradeCancel = mock(ApproveTradeCancelUseCase.class);
        adjustGwangsan = mock(AdjustGwangsanUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(findAlerts, updateMemberRole, signIn,
                        updateMemberStatus, rejectAlert, verifySignUp, deleteAlert, approveTradeCancel, adjustGwangsan))
                .setCustomArgumentResolvers(new FixedActorResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("알림 조회는")
    class AlertFilters {
        @Test
        void it_passes_absent_filters_as_null_and_serializes_empty_groups() throws Exception {
            when(findAlerts.query(new AdminAlertSearch(ADMIN, null, null)))
                    .thenReturn(new AdminAlertsResult(List.of(), List.of(), List.of()));

            mvc.perform(get("/api/admin/alert"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reports").isArray())
                    .andExpect(jsonPath("$.signUps").isArray())
                    .andExpect(jsonPath("$.tradeCancels").isArray());

            verify(findAlerts).query(new AdminAlertSearch(ADMIN, null, null));
        }

        @Test
        void it_binds_alert_type_and_place_filter() throws Exception {
            when(findAlerts.query(new AdminAlertSearch(ADMIN, 7, AlertType.SIGN_UP)))
                    .thenReturn(new AdminAlertsResult(List.of(), List.of(), List.of()));

            mvc.perform(get("/api/admin/alert").param("alert_type", "SIGN_UP").param("place_id", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.signUps").isArray());
            verify(findAlerts).query(new AdminAlertSearch(ADMIN, 7, AlertType.SIGN_UP));
        }

        @Test
        void it_serializes_alert_groups_with_existing_json_names() throws Exception {
            LocalDateTime at = LocalDateTime.of(2026, 10, 14, 9, 30);
            AdminProduct product = new AdminProduct(30L, "상품", "설명", 5000, "OBJECT", "GIVER",
                    new AdminProduct.Owner(4L, "판매자", "지점", 2), List.of(new AdminImage(9L, "p.jpg")), true, false);
            when(findAlerts.query(any())).thenReturn(new AdminAlertsResult(
                    List.of(new AdminAlertsResult.ReportAlert(1L, "신고자", null, "알 수 없음", true, 30L, "상품",
                            "기타 신고", "지점", at, 11L, "ETC", "내용", List.of(new AdminImage(8L, "r.jpg")))),
                    List.of(new AdminAlertsResult.SignUpAlert(2L, 5L, "가입자", "새로운 회원가입 요청", "지점", null, at)),
                    List.of(new AdminAlertsResult.TradeCancelAlert(3L, "요청자", "거래 철회 요청", "사유", "지점", at,
                            List.of(), product))));

            mvc.perform(get("/api/admin/alert"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reports[0].reportedMemberId").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.reports[0].reportedMemberName").value("알 수 없음"))
                    .andExpect(jsonPath("$.reports[0].targetType").value("PRODUCT"))
                    .andExpect(jsonPath("$.reports[0].report.reportType").value("ETC"))
                    .andExpect(jsonPath("$.reports[0].report.images[0].imageUrl").value("r.jpg"))
                    .andExpect(jsonPath("$.signUps[0].memberId").value(5))
                    .andExpect(jsonPath("$.tradeCancels[0].product.content").value("설명"))
                    .andExpect(jsonPath("$.tradeCancels[0].product.isCompleted").value(true))
                    .andExpect(jsonPath("$.tradeCancels[0].product.member.light").value(2))
                    .andExpect(jsonPath("$.tradeCancels[0].product.images[0].imageId").value(9));
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

            verify(updateMemberRole).execute(new UpdateMemberRoleCommand(ADMIN, 11L, ActorContext.Role.ROLE_PLACE_ADMIN, 3));
        }

        @Test
        void it_binds_member_status_and_returns_no_content() throws Exception {
            mvc.perform(patch("/api/admin/status/12").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"SUSPENDED\"}"))
                    .andExpect(status().isNoContent());

            verify(updateMemberStatus).execute(new UpdateMemberStatusCommand(ADMIN, 12L, AccountStatus.SUSPENDED));
        }

        @Test
        void it_binds_gwangsan_adjustment_and_returns_empty_ok_response() throws Exception {
            mvc.perform(patch("/api/admin/gwangsan/13").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"gwangsan\":5000}"))
                    .andExpect(status().isOk()).andExpect(content().string(""));

            verify(adjustGwangsan).execute(new AdjustGwangsanCommand(ADMIN, 13L, 5000));
        }

        @Test
        void it_routes_alert_rejection_to_no_content() throws Exception {
            mvc.perform(delete("/api/admin/alert/14")).andExpect(status().isNoContent()).andExpect(content().string(""));
            verify(rejectAlert).execute(new AdminAlertCommand(ADMIN, 14L));
        }

        @Test
        void it_routes_signup_verification_to_no_content() throws Exception {
            mvc.perform(patch("/api/admin/verify/signup/15")).andExpect(status().isNoContent()).andExpect(content().string(""));
            verify(verifySignUp).execute(new AdminAlertCommand(ADMIN, 15L));
        }

        @Test
        void it_routes_alert_deletion_to_no_content() throws Exception {
            mvc.perform(delete("/api/admin/16")).andExpect(status().isNoContent()).andExpect(content().string(""));
            verify(deleteAlert).execute(new AdminAlertCommand(ADMIN, 16L));
        }

        @Test
        void it_routes_trade_approval_to_empty_ok_response() throws Exception {
            mvc.perform(patch("/api/admin/trade/17")).andExpect(status().isOk()).andExpect(content().string(""));
            verify(approveTradeCancel).execute(new AdminAlertCommand(ADMIN, 17L));
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
            when(signIn.execute(new AdminSignInCommand("admin", "password")))
                    .thenReturn(new AdminSignInResult("access", "refresh", null, null, ActorContext.Role.ROLE_HEAD_ADMIN));

            mvc.perform(post("/api/admin/signin").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"admin\",\"password\":\"password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token.accessToken").value("access"))
                    .andExpect(jsonPath("$.token.refreshToken").value("refresh"))
                    .andExpect(jsonPath("$.role").value("ROLE_HEAD_ADMIN"));

            verify(signIn).execute(new AdminSignInCommand("admin", "password"));
        }

        @Test
        void it_maps_rejected_alert_not_found_to_error_json() throws Exception {
            doThrow(new NotFoundAdminAlertException()).when(rejectAlert).execute(new AdminAlertCommand(ADMIN, 99L));

            mvc.perform(delete("/api/admin/alert/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("존재하지 않는 어드민 알림입니다."));
        }

        @Test
        void it_maps_trade_approval_not_found_to_error_json() throws Exception {
            doThrow(new NotFoundAdminAlertException()).when(approveTradeCancel).execute(new AdminAlertCommand(ADMIN, 99L));

            mvc.perform(patch("/api/admin/trade/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("존재하지 않는 어드민 알림입니다."));
        }
    }
}
