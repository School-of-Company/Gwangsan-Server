package team.startup.gwangsan.domain.alert.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.alert.presentation.dto.response.ExistsAlertResponse;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExistsUnreadAlertServiceImpl 단위 테스트")
class ExistsUnreadAlertServiceImplTest {

    @Mock
    private AlertReceiptRepository alertReceiptRepository;

    @Mock
    private MemberUtil memberUtil;

    @InjectMocks
    private ExistsUnreadAlertServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("읽지 않은 알림이 있으면 exists=true 를 반환한다")
        void it_returns_true_when_unread_exists() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(alertReceiptRepository.existsByMemberIdAndChecked(1L, false)).thenReturn(true);

            ExistsAlertResponse result = service.execute();

            assertThat(result.unread()).isTrue();
        }

        @Test
        @DisplayName("읽지 않은 알림이 없으면 exists=false 를 반환한다")
        void it_returns_false_when_no_unread() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(alertReceiptRepository.existsByMemberIdAndChecked(1L, false)).thenReturn(false);

            ExistsAlertResponse result = service.execute();

            assertThat(result.unread()).isFalse();
        }
    }
}
