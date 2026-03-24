package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithDrawnMemberServiceImpl 단위 테스트")
class WithDrawnMemberServiceImplTest {

    @InjectMocks
    private WithDrawnMemberServiceImpl service;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("alertId가 있을 때")
        class Context_with_alert_id {

            @Test
            @DisplayName("회원 상태를 WITHDRAWN으로 변경하고 이벤트를 발행한다")
            void it_withdraws_member_and_publishes_event() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                Member targetMember = mock(Member.class);
                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getMember()).thenReturn(targetMember);

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(3L);
                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getRequester()).thenReturn(requester);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));
                when(adminAlertRepository.findById(10L)).thenReturn(Optional.of(alert));

                service.execute(2L, 10L);

                verify(targetMember).updateMemberStatus(MemberStatus.WITHDRAWN);
                verify(applicationEventPublisher).publishEvent(any(CreateAlertEvent.class));
            }

            @Test
            @DisplayName("Alert가 없으면 NotFoundAdminAlertException을 던진다")
            void it_throws_not_found_alert_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);
                MemberDetail targetDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));
                when(adminAlertRepository.findById(10L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(2L, 10L))
                        .isInstanceOf(NotFoundAdminAlertException.class);
            }
        }

        @Nested
        @DisplayName("alertId가 null일 때")
        class Context_without_alert_id {

            @Test
            @DisplayName("회원 상태 변경 없이 종료한다")
            void it_does_nothing() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);
                MemberDetail targetDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));

                service.execute(2L, null);

                verify(applicationEventPublisher, never()).publishEvent(any());
                verify(adminAlertRepository, never()).findById(any());
            }
        }
    }
}
