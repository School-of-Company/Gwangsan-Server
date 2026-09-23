package team.startup.gwangsan.domain.place.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Place 단위 테스트")
class PlaceTest {

    private Place placeWithId(Integer id) {
        Place place = Place.builder().name("지점-" + id).build();
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    @Nested
    @DisplayName("isSamePlace() 메서드는")
    class Describe_isSamePlace {

        @Test
        @DisplayName("영속된 ID가 같으면 true를 반환한다")
        void it_returns_true_for_same_persisted_id() {
            assertThat(placeWithId(1).isSamePlace(placeWithId(1))).isTrue();
        }

        @Test
        @DisplayName("영속된 ID가 다르면 false를 반환한다")
        void it_returns_false_for_different_persisted_id() {
            assertThat(placeWithId(1).isSamePlace(placeWithId(2))).isFalse();
        }

        @Test
        @DisplayName("비교 대상이 없으면 false를 반환한다")
        void it_returns_false_for_null_other() {
            assertThat(placeWithId(1).isSamePlace(null)).isFalse();
        }
    }
}
