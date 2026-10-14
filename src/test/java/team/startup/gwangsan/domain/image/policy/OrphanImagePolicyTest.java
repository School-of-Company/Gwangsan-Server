package team.startup.gwangsan.domain.image.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.image.model.Image;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrphanImagePolicy 순수 규칙")
class OrphanImagePolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 3, 0);

    @Test
    @DisplayName("it_cutoff 는 2일 전이고 cutoff 와 같은 시각·createdAt 없는 이미지는 만료가 아니다")
    void it_keeps_boundary_and_missing_created_at() {
        LocalDateTime cutoff = OrphanImagePolicy.cutoff(NOW);
        Image before = new Image(1L, "a", cutoff.minusNanos(1));
        Image exact = new Image(2L, "b", cutoff);
        Image missing = new Image(3L, "c", null);

        assertThat(cutoff).isEqualTo(NOW.minusDays(2));
        assertThat(OrphanImagePolicy.expired(List.of(before, exact, missing), cutoff)).containsExactly(before);
    }

    @Test
    @DisplayName("it_저장소 삭제 실패 URL 을 제외하고 순서를 유지한다")
    void it_excludes_failed_urls_in_order() {
        Image first = new Image(1L, "a", NOW);
        Image failed = new Image(2L, "b", NOW);
        Image last = new Image(3L, "c", NOW);

        assertThat(OrphanImagePolicy.deletable(List.of(first, failed, last), List.of("b"))).containsExactly(first, last);
    }

    @Test
    @DisplayName("it_정리 상한은 500건이다")
    void it_limits_to_five_hundred() {
        assertThat(OrphanImagePolicy.MAX_DELETE_COUNT).isEqualTo(500);
    }
}
