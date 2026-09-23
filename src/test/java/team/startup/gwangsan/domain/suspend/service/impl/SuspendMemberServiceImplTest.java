package team.startup.gwangsan.domain.suspend.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.suspend.entity.Suspend;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
                Member member = actualMember();

                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
                when(suspendRepository.save(any(Suspend.class))).thenAnswer(invocation -> {
                    Suspend suspend = invocation.getArgument(0);
                    ReflectionTestUtils.setField(suspend, "id", 10L);
                    return suspend;
                });
                when(adminAlertRepository.existsById(5L)).thenReturn(false);

                service.execute(1L, 7, 5L);

                ArgumentCaptor<Suspend> suspendCaptor = ArgumentCaptor.forClass(Suspend.class);
                verify(suspendRepository).save(suspendCaptor.capture());
                Suspend savedSuspend = suspendCaptor.getValue();
                assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
                assertThat(savedSuspend.getMember()).isSameAs(member);
                assertThat(savedSuspend.getSuspendedDays()).isEqualTo(7);
                assertThat(savedSuspend.getSuspendedAt()).isNotNull();
                assertThat(savedSuspend.getSuspendedUntil()).isEqualTo(savedSuspend.getSuspendedAt().plusDays(7));

                ArgumentCaptor<CreateAlertEvent> eventCaptor = ArgumentCaptor.forClass(CreateAlertEvent.class);
                verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
                CreateAlertEvent event = eventCaptor.getValue();
                assertThat(event.sourceId()).isEqualTo(5L);
                assertThat(event.memberId()).isEqualTo(1L);
                assertThat(event.alertType()).isEqualTo(AlertType.REPORT);
                assertThat(event.suspendId()).isEqualTo(10L);
            }
        }

        @Nested
        @DisplayName("alertId가 null일 때")
        class Context_with_null_alert_id {

            @Test
            @DisplayName("정지 처리만 수행하고 이벤트를 발행하지 않는다")
            void it_suspends_member_without_publishing_event() {
                Member member = actualMember();

                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
                when(suspendRepository.save(any(Suspend.class))).thenAnswer(invocation -> invocation.getArgument(0));

                service.execute(1L, 7, null);

                verify(suspendRepository).save(any(Suspend.class));
                assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
                verify(applicationEventPublisher, never()).publishEvent(any());
            }
        }

        @Nested
        @DisplayName("alertId가 있지만 AdminAlert이 이미 존재할 때")
        class Context_with_alert_id_and_existing_admin_alert {

            @Test
            @DisplayName("정지 처리만 수행하고 이벤트를 발행하지 않는다")
            void it_suspends_member_without_publishing_event() {
                Member member = actualMember();

                when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
                when(suspendRepository.save(any(Suspend.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(adminAlertRepository.existsById(5L)).thenReturn(true);

                service.execute(1L, 7, 5L);

                verify(suspendRepository).save(any(Suspend.class));
                assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
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

    private Member actualMember() {
        return Member.builder()
                .name("회원")
                .nickname("member")
                .phoneNumber("010-0000-0001")
                .password("password")
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
