package team.startup.gwangsan.adapter.integration.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.AddMemberLightUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewMemberAdapter: review → member 공개 Port")
class ReviewMemberAdapterTest {

    @InjectMocks private ReviewMemberAdapter adapter;

    @Mock private MemberAccountQuery memberAccountQuery;
    @Mock private AddMemberLightUseCase addMemberLightUseCase;

    @Test
    void it_adds_light_through_member_use_case_with_same_arguments() {
        adapter.addLight(8L, 30);
        adapter.addLight(9L, null);

        verify(addMemberLightUseCase).execute(8L, 30);
        verify(addMemberLightUseCase).execute(9L, null);
    }

    @Test
    void it_propagates_missing_member_detail_so_review_rolls_back() {
        doThrow(new NotFoundMemberDetailException()).when(addMemberLightUseCase).execute(8L, 30);

        assertThatThrownBy(() -> adapter.addLight(8L, 30)).isInstanceOf(NotFoundMemberDetailException.class);
    }

    @Test
    void it_reads_nickname_or_throws_not_found_member() {
        when(memberAccountQuery.findById(8L)).thenReturn(Optional.of(
                new MemberAccount(8L, "대상", "010", "hash", MemberRole.ROLE_USER, MemberStatus.ACTIVE)));
        when(memberAccountQuery.findById(9L)).thenReturn(Optional.empty());

        assertThat(adapter.findNickname(8L)).isEqualTo("대상");
        assertThatThrownBy(() -> adapter.findNickname(9L)).isInstanceOf(NotFoundMemberException.class);
    }
}
