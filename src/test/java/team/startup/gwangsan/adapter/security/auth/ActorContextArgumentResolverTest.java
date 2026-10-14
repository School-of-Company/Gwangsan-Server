package team.startup.gwangsan.adapter.security.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.support.MemberAccounts;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ActorContext 인증 경계")
class ActorContextArgumentResolverTest {

    private MockMvc mvc;

    @RestController
    static class ActorEchoController {
        @GetMapping("/actor")
        Map<String, Object> actor(ActorContext actor) {
            return Map.of("memberId", actor.memberId(), "role", actor.role().name());
        }

        // body 의 memberId 는 실행자가 아니다.
        @PostMapping("/actor")
        Map<String, Object> actorWithBody(ActorContext actor, @RequestBody Map<String, Object> body) {
            return Map.of("memberId", actor.memberId(), "bodyMemberId", body.get("memberId"));
        }
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ActorEchoController())
                .setCustomArgumentResolvers(new ActorContextArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("MemberDetails principal이 있으면")
    class Context_authenticated {

        @ParameterizedTest
        @EnumSource(MemberRole.class)
        @DisplayName("it_principal의 회원 ID와 같은 이름의 역할을 전달한다")
        void it_resolves_member_id_and_role(MemberRole role) throws Exception {
            authenticate(member(7L, role));

            mvc.perform(get("/actor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(7))
                    .andExpect(jsonPath("$.role").value(role.name()));
        }

        @Test
        @DisplayName("it_요청 query/body의 회원 ID로 실행자를 바꾸지 않는다")
        void it_ignores_forged_member_id() throws Exception {
            authenticate(member(7L, MemberRole.ROLE_USER));

            mvc.perform(post("/actor?memberId=999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"memberId\":999}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(7))
                    .andExpect(jsonPath("$.bodyMemberId").value(999));
        }
    }

    @Nested
    @DisplayName("인증 principal이 없거나 MemberDetails가 아니면")
    class Context_unauthenticated {

        @Test
        @DisplayName("it_인증이 없으면 기존 INVALID_MEMBER_PRINCIPAL 401을 반환한다")
        void it_returns_401_without_authentication() throws Exception {
            mvc.perform(get("/actor"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("현재 인증된 사용자의 정보가 유효하지 않습니다."));
        }

        @Test
        @DisplayName("it_principal이 문자열이면 401을 반환한다")
        void it_returns_401_for_non_member_principal() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("010-0000-0000", null, java.util.List.of()));

            mvc.perform(get("/actor"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private void authenticate(MemberJpaEntity member) {
        MemberDetails details = new MemberDetails(MemberAccounts.of(member));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private MemberJpaEntity member(Long id, MemberRole role) {
        MemberJpaEntity member = MemberJpaEntity.builder()
                .name("실행자")
                .nickname("actor")
                .phoneNumber("010-1234-5678")
                .password("encoded")
                .role(role)
                .status(MemberStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
