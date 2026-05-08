package team.startup.gwangsan.domain.suspend.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.suspend.entity.Suspend;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuspendMemberServiceImpl 단위 테스트")
class SuspendMemberServiceImplTest {

    @InjectMocks
    private SuspendMemberServiceImpl service;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SuspendRepository suspendRepository;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("alertId가 있고 AdminAlert이 존재하지 않을 때")
        class Context_with_alert_id_and_no_admin_alert {

            @Test
            @DisplayName("정지 처리 후 이벤트를 발행한다")
            void it_suspends_member_and_publishes_event() {
                Member member = mock(Member.class);
                Suspend savedSuspend = mock(Suspend.class);
                when(savedSuspend.getId()).thenReturn(10L);

                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
                when(suspendRepository.save(any(Suspend.class))).thenReturn(savedSuspend);
                when(adminAlertRepository.existsById(5L)).thenReturn(false);

                service.execute(1L, 7, 5L);

                verify(suspendRepository).save(any(Suspend.class));
                verify(member).updateMemberStatus(MemberStatus.SUSPENDED);
                verify(applicationEventPublisher).publishEvent(any(CreateAlertEvent.class));
            }
        }

        @Nested
        @DisplayName("alertId가 null일 때")
        class Context_with_null_alert_id {

            @Test
            @DisplayName("정지 처리만 수행하고 이벤트를 발행하지 않는다")
            void it_suspends_member_without_publishing_event() {
                Member member = mock(Member.class);
                Suspend savedSuspend = mock(Suspend.class);

                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
                when(suspendRepository.save(any(Suspend.class))).thenReturn(savedSuspend);

                service.execute(1L, 7, null);

                verify(suspendRepository).save(any(Suspend.class));
                verify(member).updateMemberStatus(MemberStatus.SUSPENDED);
                verify(applicationEventPublisher, never()).publishEvent(any());
            }
        }

        @Nested
        @DisplayName("alertId가 있지만 AdminAlert이 이미 존재할 때")
        class Context_with_alert_id_and_existing_admin_alert {

            @Test
            @DisplayName("정지 처리만 수행하고 이벤트를 발행하지 않는다")
            void it_suspends_member_without_publishing_event() {
                Member member = mock(Member.class);
                Suspend savedSuspend = mock(Suspend.class);

                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
                when(suspendRepository.save(any(Suspend.class))).thenReturn(savedSuspend);
                when(adminAlertRepository.existsById(5L)).thenReturn(true);

                service.execute(1L, 7, 5L);

                verify(suspendRepository).save(any(Suspend.class));
                verify(member).updateMemberStatus(MemberStatus.SUSPENDED);
                verify(applicationEventPublisher, never()).publishEvent(any());
            }
        }

        @Nested
        @DisplayName("멤버가 존재하지 않을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                when(memberRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(99L, 7, 5L))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
