package team.startup.gwangsan.adapter.integration.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.block.port.in.BlockRelationQuery;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.post.data.AuthorProfile;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostMemberBridgeAdapter 는 회원 요약 query 를 상세 기준으로 쓴다")
class PostMemberBridgeAdapterTest {

    @InjectMocks private PostMemberBridgeAdapter adapter;
    @Mock private MemberAccountQuery memberAccountQuery;
    @Mock private MemberSummaryQuery memberSummaryQuery;
    @Mock private BlockRelationQuery blockRelationQuery;

    private static MemberSummary withDetail(Long id) {
        return new MemberSummary(id, "닉" + id, "010", null, null, 5, 1, "광산", 35);
    }

    private static MemberSummary withoutDetail(Long id) {
        return new MemberSummary(id, "닉" + id, "010", null, null, null, null, null, null);
    }

    @Nested
    @DisplayName("단건 조회는")
    class Describe_single {

        @Test
        @DisplayName("지점 ID 와 작성자 프로필을 요약에서 만든다")
        void it_maps_place_id_and_profile() {
            when(memberSummaryQuery.findById(2L)).thenReturn(Optional.of(withDetail(2L)));

            assertThat(adapter.placeIdOf(2L)).isEqualTo(5);
            assertThat(adapter.profile(2L)).isEqualTo(new AuthorProfile(2L, "닉2", "광산", 35));
        }

        @Test
        @DisplayName("회원이 없거나 상세가 없으면 지점은 NotFoundMemberException, 프로필은 NotFoundMemberDetailException")
        void it_throws_when_detail_missing() {
            when(memberSummaryQuery.findById(2L)).thenReturn(Optional.of(withoutDetail(2L)));
            when(memberSummaryQuery.findById(3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.placeIdOf(2L)).isInstanceOf(NotFoundMemberException.class);
            assertThatThrownBy(() -> adapter.placeIdOf(3L)).isInstanceOf(NotFoundMemberException.class);
            assertThatThrownBy(() -> adapter.profile(2L)).isInstanceOf(NotFoundMemberDetailException.class);
            assertThatThrownBy(() -> adapter.profile(3L)).isInstanceOf(NotFoundMemberDetailException.class);
        }
    }

    @Test
    @DisplayName("묶음 프로필은 상세가 있는 회원만 회원 ID 로 색인한다")
    void it_indexes_profiles_with_detail() {
        when(memberSummaryQuery.findAllByIds(List.of(2L, 3L))).thenReturn(List.of(withDetail(2L), withoutDetail(3L)));

        Map<Long, AuthorProfile> profiles = adapter.profiles(List.of(2L, 3L));

        assertThat(profiles).containsOnlyKeys(2L);
        assertThat(profiles.get(2L)).isEqualTo(new AuthorProfile(2L, "닉2", "광산", 35));
    }
}
