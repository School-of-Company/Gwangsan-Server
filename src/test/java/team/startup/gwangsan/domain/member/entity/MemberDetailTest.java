package team.startup.gwangsan.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberDetail 단위 테스트")
class MemberDetailTest {

    private MemberDetail memberDetail(Integer gwangsan, Integer light) {
        return MemberDetail.builder().gwangsan(gwangsan).light(light).build();
    }

    @Nested
    @DisplayName("adjustGwangsan() 메서드는")
    class Describe_adjustGwangsan {

        @Test
        @DisplayName("양수와 음수 조정 후 잔액을 반영한다")
        void it_adjusts_gwangsan_with_positive_and_negative_delta() {
            MemberDetail detail = memberDetail(100, 0);

            detail.adjustGwangsan(50);
            detail.adjustGwangsan(-30);

            assertThat(detail.getGwangsan()).isEqualTo(120);
        }

        @Test
        @DisplayName("정확히 0이 되는 조정을 유지한다")
        void it_keeps_exactly_zero() {
            MemberDetail detail = memberDetail(100, 0);

            detail.adjustGwangsan(-100);

            assertThat(detail.getGwangsan()).isZero();
        }

        @Test
        @DisplayName("0 미만 조정은 0으로 제한한다")
        void it_clamps_negative_balance_to_zero() {
            MemberDetail detail = memberDetail(100, 0);

            detail.adjustGwangsan(-101);

            assertThat(detail.getGwangsan()).isZero();
        }
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
