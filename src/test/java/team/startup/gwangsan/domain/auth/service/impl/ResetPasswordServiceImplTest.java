package team.startup.gwangsan.domain.auth.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.ResetPasswordRequest;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.redis.RedisUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResetPasswordServiceImpl 단위 테스트")
class ResetPasswordServiceImplTest {

    @InjectMocks
    private ResetPasswordServiceImpl service;

    @Mock private MemberRepository memberRepository;
    @Mock private RedisUtil redisUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 비밀번호 재설정 시")
        class Context_with_valid_request {

            @Test
            @DisplayName("비밀번호를 변경하고 Redis 인증 키를 삭제한다")
            void it_changes_password_and_deletes_redis_key() {
                ResetPasswordRequest request = new ResetPasswordRequest("01012345678", "newPw");
                Member member = mock(Member.class);

                when(redisUtil.get("sms:verified:01012345678", Boolean.class)).thenReturn(true);
                when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.of(member));
                when(passwordEncoder.encode("newPw")).thenReturn("encodedNewPw");

                service.execute(request);

                verify(member).changePassword("encodedNewPw");
                verify(redisUtil).delete("sms:verified:01012345678");
            }
        }

        @Nested
        @DisplayName("SMS 인증 정보가 Redis에 없을 때")
        class Context_with_sms_not_found {

            @Test
            @DisplayName("SmsAuthNotFoundException을 던진다")
            void it_throws_sms_not_found_exception() {
                ResetPasswordRequest request = new ResetPasswordRequest("01012345678", "newPw");
                when(redisUtil.get("sms:verified:01012345678", Boolean.class)).thenReturn(null);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(SmsAuthNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("SMS 인증이 완료되지 않았을 때")
        class Context_with_sms_not_completed {

            @Test
            @DisplayName("SmsAuthNotCompletedException을 던진다")
            void it_throws_sms_not_completed_exception() {
                ResetPasswordRequest request = new ResetPasswordRequest("01012345678", "newPw");
                when(redisUtil.get("sms:verified:01012345678", Boolean.class)).thenReturn(false);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(SmsAuthNotCompletedException.class);
            }
        }

        @Nested
        @DisplayName("회원이 존재하지 않을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                ResetPasswordRequest request = new ResetPasswordRequest("01099999999", "newPw");
                when(redisUtil.get("sms:verified:01099999999", Boolean.class)).thenReturn(true);
                when(memberRepository.findByPhoneNumber("01099999999")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
