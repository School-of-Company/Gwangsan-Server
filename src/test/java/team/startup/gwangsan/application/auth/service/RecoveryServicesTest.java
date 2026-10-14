package team.startup.gwangsan.application.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.auth.data.ResetPasswordCommand;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.PasswordHashPort;
import team.startup.gwangsan.application.auth.port.out.PhoneVerificationPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundMemberAccountException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.auth.model.AccountStatus;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.PHONE;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.account;

/** 비밀번호 재설정·별칭 찾기. 둘 다 SMS 인증 완료 표시를 확인하고 소비한다. */
@ExtendWith(MockitoExtension.class)
@DisplayName("계정 복구 서비스 단위 테스트")
class RecoveryServicesTest {

    @Mock private PhoneVerificationPort phoneVerificationPort;
    @Mock private AccountPort accountPort;
    @Mock private PasswordHashPort passwordHashPort;

    private ResetPasswordService resetPassword;
    private FindNicknameService findNickname;

    @BeforeEach
    void setUp() {
        resetPassword = new ResetPasswordService(phoneVerificationPort, accountPort, passwordHashPort);
        findNickname = new FindNicknameService(phoneVerificationPort, accountPort);
    }

    @Nested
    @DisplayName("ResetPasswordService.execute() 는")
    class Describe_reset_password {

        @Test
        @DisplayName("비밀번호를 변경한 뒤 인증 표시를 소비한다")
        void it_changes_password_and_consumes_verification() {
            when(phoneVerificationPort.findVerified(PHONE)).thenReturn(true);
            when(accountPort.findByPhoneNumber(PHONE)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
            when(passwordHashPort.encode("newPw")).thenReturn("encodedNewPw");

            resetPassword.execute(new ResetPasswordCommand(PHONE, "newPw"));

            InOrder order = inOrder(accountPort, phoneVerificationPort);
            order.verify(accountPort).changePassword(1L, "encodedNewPw");
            order.verify(phoneVerificationPort).consume(PHONE);
        }

        @Test
        @DisplayName("인증 정보가 없으면 SmsAuthNotFoundException을 던진다")
        void it_throws_sms_not_found_exception() {
            when(phoneVerificationPort.findVerified(PHONE)).thenReturn(null);

            assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(PHONE, "newPw")))
                    .isInstanceOf(SmsAuthNotFoundException.class);
            verifyNoInteractions(accountPort);
        }

        @Test
        @DisplayName("인증이 완료되지 않았으면 SmsAuthNotCompletedException을 던진다")
        void it_throws_sms_not_completed_exception() {
            when(phoneVerificationPort.findVerified(PHONE)).thenReturn(false);

            assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand(PHONE, "newPw")))
                    .isInstanceOf(SmsAuthNotCompletedException.class);
        }

        @Test
        @DisplayName("회원이 없으면 기존과 같은 NOT_FOUND_MEMBER 오류를 던지고 인증 표시를 남긴다")
        void it_throws_not_found_member_exception() {
            when(phoneVerificationPort.findVerified("01099999999")).thenReturn(true);
            when(accountPort.findByPhoneNumber("01099999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resetPassword.execute(new ResetPasswordCommand("01099999999", "newPw")))
                    .isInstanceOf(NotFoundMemberAccountException.class)
                    .extracting(e -> ((GlobalException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_MEMBER);
            verify(phoneVerificationPort, never()).consume(any());
        }
    }

    @Nested
    @DisplayName("FindNicknameService.query() 는")
    class Describe_find_nickname {

        @Test
        @DisplayName("별칭을 반환하고 인증 표시를 소비한다")
        void it_returns_nickname_and_consumes_verification() {
            when(phoneVerificationPort.findVerified(PHONE)).thenReturn(true);
            when(accountPort.findByPhoneNumber(PHONE)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));

            assertThat(findNickname.query(PHONE)).isEqualTo("테스터일");
            verify(phoneVerificationPort).consume(PHONE);
        }

        @Test
        @DisplayName("인증 정보가 없으면 SmsAuthNotFoundException을 던진다")
        void it_throws_sms_not_found_exception() {
            when(phoneVerificationPort.findVerified(PHONE)).thenReturn(null);

            assertThatThrownBy(() -> findNickname.query(PHONE)).isInstanceOf(SmsAuthNotFoundException.class);
        }

        @Test
        @DisplayName("인증이 완료되지 않았으면 SmsAuthNotCompletedException을 던진다")
        void it_throws_sms_not_completed_exception() {
            when(phoneVerificationPort.findVerified(PHONE)).thenReturn(false);

            assertThatThrownBy(() -> findNickname.query(PHONE)).isInstanceOf(SmsAuthNotCompletedException.class);
        }

        @Test
        @DisplayName("회원이 없으면 NOT_FOUND_MEMBER 오류를 던진다")
        void it_throws_not_found_member_exception() {
            when(phoneVerificationPort.findVerified("01099999999")).thenReturn(true);
            when(accountPort.findByPhoneNumber("01099999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findNickname.query("01099999999"))
                    .isInstanceOf(NotFoundMemberAccountException.class)
                    .extracting(e -> ((GlobalException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_MEMBER);
        }
    }
}
