package team.startup.gwangsan.domain.member.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberDetail 순수 모델")
class MemberDetailTest {

    private MemberDetail memberDetail(Integer gwangsan, Integer light) {
        return new MemberDetail(1L, 1, 1, gwangsan, light, "소개");
    }

    @Nested
    @DisplayName("plusLight() 메서드는")
    class Describe_plusLight {

        @Test
        @DisplayName("하한 0을 유지한다")
        void it_clamps_below_zero() {
            MemberDetail detail = memberDetail(0, 0);

            detail.plusLight(-1);

            assertThat(detail.getLight()).isZero();
        }

        @Test
        @DisplayName("상한 100을 유지한다")
        void it_clamps_above_one_hundred() {
            MemberDetail detail = memberDetail(0, 100);

            detail.plusLight(1);

            assertThat(detail.getLight()).isEqualTo(100);
        }

        @Test
        @DisplayName("범위 안의 점수는 그대로 더한다")
        void it_adds_light_within_boundaries() {
            MemberDetail detail = memberDetail(0, 50);

            detail.plusLight(10);

            assertThat(detail.getLight()).isEqualTo(60);
        }

        @Test
        @DisplayName("기존 점수와 증감값이 null이면 0으로 처리한다")
        void it_treats_null_light_and_delta_as_zero() {
            MemberDetail nullLight = memberDetail(0, null);
            MemberDetail existingLight = memberDetail(0, 50);

            nullLight.plusLight(null);
            existingLight.plusLight(null);

            assertThat(nullLight.getLight()).isZero();
            assertThat(existingLight.getLight()).isEqualTo(50);
        }
    }
}
