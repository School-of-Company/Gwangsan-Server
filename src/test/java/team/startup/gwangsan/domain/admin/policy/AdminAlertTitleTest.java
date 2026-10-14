package team.startup.gwangsan.domain.admin.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("관리자 알림 제목·가입 보상 정책")
class AdminAlertTitleTest {

    @ParameterizedTest
    @CsvSource({
            "SEXUAL,음란/성적 콘텐츠 신고",
            "ABUSE_HATE_HARASSMENT,욕설/혐오/괴롭힘 신고",
            "SPAM_AD,스팸/광고 신고",
            "IMPERSONATION,사칭 신고",
            "SELF_HARM_DANGER,자해/위험 신고",
            "ETC,기타 신고"
    })
    @DisplayName("it_신고 종류별 기존 제목을 유지한다")
    void it_keeps_report_titles(AdminAlertTitle.ReportCategory category, String title) {
        assertThat(AdminAlertTitle.report(category)).isEqualTo(title);
    }

    @Test
    @DisplayName("it_탈퇴 잔액이 있으면 복원하고 없으면 10000 을 준다")
    void it_restores_or_rewards() {
        assertThat(SignUpReward.gwangsanFor(Optional.of(3200))).isEqualTo(3200);
        assertThat(SignUpReward.gwangsanFor(Optional.empty())).isEqualTo(10000);
        assertThat(SignUpReward.RECOMMENDER_GWANGSAN_REWARD).isEqualTo(5000);
    }
}
