package team.startup.gwangsan.domain.sms.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SmsAuthPolicy")
class SmsAuthPolicyTest {

    @Test
    @DisplayName("it_6자리 숫자 코드와 기존 TTL·최대 시도 횟수를 유지한다")
    void it_keeps_existing_code_rules() {
        assertThat(SmsAuthPolicy.generateCode()).matches("\\d{6}");
        assertThat(SmsAuthPolicy.CODE_TTL).isEqualTo(Duration.ofMillis(3 * 60 * 1000));
        assertThat(SmsAuthPolicy.VERIFIED_TTL).isEqualTo(Duration.ofMillis(10 * 60 * 1000L));
        assertThat(SmsAuthPolicy.nextAttempt(null)).isEqualTo(1);
        assertThat(SmsAuthPolicy.nextAttempt(4)).isEqualTo(5);
        assertThatThrownBy(() -> SmsAuthPolicy.nextAttempt(5)).isInstanceOf(TooManyRequestAuthCodeException.class);
        assertThat(SmsAuthPolicy.mask("01012345678")).isEqualTo("010****5678");
    }
}
