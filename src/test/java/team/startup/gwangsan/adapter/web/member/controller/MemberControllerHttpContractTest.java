package team.startup.gwangsan.adapter.web.member.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.application.member.data.MyInfoResult;
import team.startup.gwangsan.application.member.data.UpdateMyInfoCommand;
import team.startup.gwangsan.application.member.data.UserInfoResult;
import team.startup.gwangsan.application.member.port.in.FindAllUserInfoQuery;
import team.startup.gwangsan.application.member.port.in.FindMyInfoQuery;
import team.startup.gwangsan.application.member.port.in.FindUserInfoQuery;
import team.startup.gwangsan.application.member.port.in.UpdateMyInfoUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.DuplicateMemberNicknameException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;
import team.startup.gwangsan.support.FixedActorResolver;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("회원 HTTP 계약")
class MemberControllerHttpContractTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN);
    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

    private MockMvc mvc;
    private FindMyInfoQuery findMyInfo;
    private UpdateMyInfoUseCase updateMyInfo;
    private FindUserInfoQuery findUserInfo;
    private FindAllUserInfoQuery findAllUserInfo;
    private WithdrawMemberUseCase withdrawal;

    @BeforeEach
    void setUp() {
        findMyInfo = mock(FindMyInfoQuery.class);
        updateMyInfo = mock(UpdateMyInfoUseCase.class);
        findUserInfo = mock(FindUserInfoQuery.class);
        findAllUserInfo = mock(FindAllUserInfoQuery.class);
        withdrawal = mock(WithdrawMemberUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new MemberController(findMyInfo, updateMyInfo, findUserInfo,
                        findAllUserInfo, withdrawal))
                .setCustomArgumentResolvers(new FixedActorResolver(ACTOR))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/member 는")
    class Describe_my_info {

        @Test
        @DisplayName("it_기존 응답의 dongName 자리에 본부 이름, headName 자리에 동 이름을 그대로 낸다")
        void it_preserves_existing_field_values() throws Exception {
            when(findMyInfo.query(ACTOR)).thenReturn(new MyInfoResult(1L, "테스터일", "수완지점", "광산구", "수완동",
                    5, 1000, "자기소개", List.of("Java")));

            mvc.perform(get("/api/member"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            {"memberId":1,"nickname":"테스터일","placeName":"수완지점","dongName":"광산구",
                             "headName":"수완동","light":5,"gwangsan":1000,"description":"자기소개","specialties":["Java"]}
                            """, true));
        }
    }

    @Nested
    @DisplayName("PATCH /api/member 는")
    class Describe_update {

        @Test
        @DisplayName("it_인증 실행자로 command 를 만들고 빈 200 을 반환한다")
        void it_updates_with_actor() throws Exception {
            mvc.perform(patch("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"새닉\",\"description\":\"설명\",\"specialties\":[\"요리\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(updateMyInfo).execute(new UpdateMyInfoCommand(ACTOR, "새닉", "설명", List.of("요리")));
        }

        @Test
        @DisplayName("it_닉네임 중복은 기존 409 오류 본문으로 응답한다")
        void it_maps_duplicate_nickname() throws Exception {
            doThrow(new DuplicateMemberNicknameException()).when(updateMyInfo).execute(any());

            mvc.perform(patch("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"중복\",\"description\":\"설명\",\"specialties\":[]}"))
                    .andExpect(status().isConflict())
                    .andExpect(content().json("{\"status\":409,\"message\":\"이미 존재하는 닉네임입니다.\"}", true));
        }
    }

    @Nested
    @DisplayName("회원 조회는")
    class Describe_queries {

        @Test
        @DisplayName("it_GET /api/member/{id} 의 필드 이름과 값을 유지한다")
        void it_serializes_user_info() throws Exception {
            when(findUserInfo.query(2L)).thenReturn(new UserInfoResult(2L, "테스터이", "수완지점", 3, 500, "설명",
                    List.of("Spring"), "이름", "01012345678", MemberRole.ROLE_USER, MemberStatus.ACTIVE, JOINED_AT));

            mvc.perform(get("/api/member/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(2))
                    .andExpect(jsonPath("$.placeName").value("수완지점"))
                    .andExpect(jsonPath("$.specialties[0]").value("Spring"))
                    .andExpect(jsonPath("$.role").value("ROLE_USER"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.phoneNumber").value("01012345678"));
        }

        @Test
        @DisplayName("it_GET /api/member/all 은 선택 query 를 넘기고 배열을 반환한다")
        void it_serializes_admin_list() throws Exception {
            when(findAllUserInfo.query(ACTOR, "회원A", null)).thenReturn(List.of(new MemberProfile(2L, "회원A", "이름",
                    "01012345678", MemberRole.ROLE_USER, MemberStatus.PENDING, JOINED_AT, "지점", null, null, 1, 100, "")));

            mvc.perform(get("/api/member/all").param("nickname", "회원A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].memberId").value(2))
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$[0].gwangsan").value(100))
                    .andExpect(jsonPath("$[0].placeName").doesNotExist());
        }
    }

    @Nested
    @DisplayName("DELETE /api/member 는")
    class Describe_withdrawal {

        @Test
        @DisplayName("it_인증 실행자 ID 로 재가입 차단 없는 탈퇴 UseCase 를 호출하고 204 를 반환한다")
        void it_calls_withdrawal_use_case() throws Exception {
            mvc.perform(delete("/api/member"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(withdrawal).execute(new WithdrawMemberCommand(ACTOR.memberId(), false));
        }
    }
}
