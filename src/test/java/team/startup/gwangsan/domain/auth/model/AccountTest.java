package team.startup.gwangsan.domain.auth.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("auth 순수 규칙")
class AccountTest {

    private Account account(AccountStatus status) {
        return new Account(1L, "n", "p", "h", "ROLE_USER", status);
    }

    @Nested
    @DisplayName("Account.assertCanSignIn() 은")
    class Describe_assert_can_sign_in {

        @Test
        @DisplayName("ACTIVE 만 통과시키고 PENDING 과 그 외 상태를 다른 오류로 거절한다")
        void it_distinguishes_pending_from_other_inactive_states() {
            assertThatCode(() -> account(AccountStatus.ACTIVE).assertCanSignIn()).doesNotThrowAnyException();
            assertThatThrownBy(() -> account(AccountStatus.PENDING).assertCanSignIn())
                    .isExactlyInstanceOf(PendingApprovalException.class);
            assertThatThrownBy(() -> account(AccountStatus.SUSPENDED).assertCanSignIn())
                    .isExactlyInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> account(AccountStatus.WITHDRAWN).assertCanSignIn())
                    .isExactlyInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("PhoneVerification.assertCompleted() 는")
    class Describe_phone_verification {

        @Test
        @DisplayName("기록 없음과 미완료를 구분한다")
        void it_distinguishes_missing_from_incomplete() {
            assertThatCode(() -> PhoneVerification.assertCompleted(true)).doesNotThrowAnyException();
            assertThatThrownBy(() -> PhoneVerification.assertCompleted(null)).isInstanceOf(SmsAuthNotFoundException.class);
            assertThatThrownBy(() -> PhoneVerification.assertCompleted(false)).isInstanceOf(SmsAuthNotCompletedException.class);
        }
    }
}
