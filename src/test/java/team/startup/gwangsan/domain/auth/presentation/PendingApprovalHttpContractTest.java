package team.startup.gwangsan.domain.auth.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import team.startup.gwangsan.domain.admin.presentation.AdminController;
import team.startup.gwangsan.domain.admin.service.*;
import team.startup.gwangsan.domain.admin.service.impl.SignInAdminServiceImpl;
import team.startup.gwangsan.domain.auth.entity.RefreshToken;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.auth.service.*;
import team.startup.gwangsan.domain.auth.service.impl.ReissueTokenServiceImpl;
import team.startup.gwangsan.domain.auth.service.impl.SignInServiceImpl;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("승인 대기 회원 실제 HTTP 계약")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PendingApprovalHttpContractTest {
    private final MemberRepository members = mock(MemberRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final DeviceTokenRepository devices = mock(DeviceTokenRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtProvider jwt = mock(JwtProvider.class);
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private AnnotationConfigServletWebServerApplicationContext context;
    private String baseUrl;

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebConfiguration {
    }

    @BeforeAll
    void startIsolatedServer() throws Exception {
        context = new AnnotationConfigServletWebServerApplicationContext();
        context.register(WebConfiguration.class);
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        factory.setAddress(InetAddress.getByName("127.0.0.1"));
        context.registerBean(TomcatServletWebServerFactory.class, () -> factory);
        context.registerBean(GlobalExceptionHandler.class, GlobalExceptionHandler::new);
        context.registerBean("dispatcherServlet", ServletRegistrationBean.class,
                () -> new ServletRegistrationBean<>(new DispatcherServlet(context), "/"));
        context.registerBean(AuthController.class, () -> new AuthController(
                mock(SignUpService.class), new SignInServiceImpl(members, passwords, jwt, refreshTokens, devices),
                new ReissueTokenServiceImpl(refreshTokens, jwt, members), mock(SignOutService.class),
                mock(TokenAuthenticationService.class), mock(ResetPasswordService.class), mock(FindNicknameService.class)));
        context.registerBean(AdminController.class, () -> new AdminController(
                mock(FindAlertByAlertTypeAndPlaceService.class), mock(UpdateMemberRoleService.class),
                new SignInAdminServiceImpl(members, passwords, jwt, refreshTokens),
                mock(UpdateMemberStatusService.class), mock(RejectAdminAlertService.class),
                mock(VerificationSignUpService.class), mock(DeleteAdminAlertService.class),
                mock(ApproveTradeCancelService.class), mock(AdjustGwangsanService.class)));
        context.refresh();
        baseUrl = "http://127.0.0.1:" + context.getWebServer().getPort();
    }

    @AfterAll
    void stopServer() {
        if (context != null) {
            context.close();
        }
        client.close();
    }

    @BeforeEach
    void resetDependencies() {
        reset(members, refreshTokens, devices, passwords, jwt);
        when(passwords.matches("password", "encoded")).thenReturn(true);
        when(refreshTokens.findByToken("refresh")).thenReturn(Optional.of(
                RefreshToken.builder().phoneNumber("01012345678").token("refresh").build()));
        when(jwt.validateRefreshToken("refresh")).thenReturn(true);
        when(jwt.generateAccessToken(any(), any())).thenReturn("new-access");
        when(jwt.generateRefreshToken(any())).thenReturn("new-refresh");
    }

    @ParameterizedTest
    @EnumSource(MemberStatus.class)
    @DisplayName("세 인증 경로에서 회원 상태별 응답 계약을 유지한다")
    void it_preserves_status_policy_for_all_three_routes(MemberStatus status) throws Exception {
        setMember(status, MemberRole.ROLE_HEAD_ADMIN);
        for (String route : new String[]{"/api/auth/signin", "/api/auth/reissue", "/api/admin/signin"}) {
            clearInvocations(jwt, refreshTokens, devices);
            HttpResponse<String> response = request(route, "password", "refresh");
            if (status == MemberStatus.ACTIVE) {
                assertThat(response.statusCode()).as(route).isEqualTo(200);
                JsonNode tokens = json.readTree(response.body());
                if (route.equals("/api/admin/signin")) {
                    assertThat(tokens.path("role").asText()).isEqualTo("ROLE_HEAD_ADMIN");
                    tokens = tokens.path("token");
                }
                assertThat(tokens.path("accessToken").asText()).isEqualTo("new-access");
                assertThat(tokens.path("refreshToken").asText()).isEqualTo("new-refresh");
                verify(refreshTokens).save(any());
            } else {
                assertError(response, 403, status == MemberStatus.PENDING
                        ? "승인 대기 중인 계정입니다. 관리자 승인 후 이용 가능합니다."
                        : "탈퇴한 회원이거나 접근이 제한된 계정입니다.");
                assertNoIssuance();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(MemberStatus.class)
    @DisplayName("잘못된 자격증명에는 회원 상태를 노출하지 않는다")
    void it_hides_account_status_for_invalid_credentials(MemberStatus status) throws Exception {
        setMember(status, MemberRole.ROLE_HEAD_ADMIN);
        when(jwt.validateRefreshToken("refresh")).thenReturn(false);
        for (String route : new String[]{"/api/auth/signin", "/api/auth/reissue", "/api/admin/signin"}) {
            assertError(request(route, "wrong", "refresh"), 401, "닉네임 또는 비밀번호가 잘못되었습니다.");
        }
        assertNoIssuance();
        verify(members, never()).findByPhoneNumber(any());
    }

    @Test
    @DisplayName("승인 대기 일반 회원에게 관리자 권한을 발급하지 않는다")
    void it_denies_admin_access_to_pending_ordinary_member() throws Exception {
        setMember(MemberStatus.PENDING, MemberRole.ROLE_USER);
        assertError(request("/api/admin/signin", "password", "refresh"), 403,
                "탈퇴한 회원이거나 접근이 제한된 계정입니다.");
        assertNoIssuance();
    }

    private void setMember(MemberStatus status, MemberRole role) {
        Member member = Member.builder().nickname("테스터").password("encoded")
                .phoneNumber("01012345678").status(status).role(role).build();
        when(members.findByNickname("테스터")).thenReturn(Optional.of(member));
        when(members.findByPhoneNumber("01012345678")).thenReturn(Optional.of(member));
    }

    private HttpResponse<String> request(String route, String password, String refreshToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + route))
                .timeout(Duration.ofSeconds(10));
        if (route.equals("/api/auth/reissue")) {
            request.header("RefreshToken", refreshToken).method("PATCH", HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                    "{\"nickname\":\"테스터\",\"password\":\"" + password + "\"}", StandardCharsets.UTF_8));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertError(HttpResponse<String> response, int status, String message) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(json.readTree(response.body())).isEqualTo(json.readTree(
                "{\"status\":" + status + ",\"message\":\"" + message + "\"}"));
    }

    private void assertNoIssuance() {
        verify(jwt, never()).generateAccessToken(any(), any());
        verify(jwt, never()).generateRefreshToken(any());
        verify(refreshTokens, never()).save(any());
        verifyNoInteractions(devices);
    }
}
