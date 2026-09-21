package team.startup.gwangsan.global.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.suspend.entity.Suspend;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuspendReleaseScheduler 단위 테스트")
class SuspendReleaseSchedulerTest {

    private SuspendRepository suspendRepository;
    private SuspendReleaseScheduler scheduler;

    @BeforeEach
    void setUp() {
        suspendRepository = mock(SuspendRepository.class);
        scheduler = new SuspendReleaseScheduler(suspendRepository);
    }

    @Nested
    @DisplayName("releaseSuspensions() 메서드는")
    class Describe_releaseSuspensions {

        @Test
        @DisplayName("만료된 정지 목록의 회원 상태를 ACTIVE로 변경한다")
        void it_releases_each_member_returned_as_expired() {
            Member first = suspendedMember("first");
            Member second = suspendedMember("second");
            when(suspendRepository.findAllBySuspendedUntilBefore(any(LocalDateTime.class)))
                    .thenReturn(List.of(expiredSuspend(first), expiredSuspend(second)));

            scheduler.releaseSuspensions();

            assertThat(first.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(second.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            verify(suspendRepository).findAllBySuspendedUntilBefore(any(LocalDateTime.class));
            verifyNoMoreInteractions(suspendRepository);
        }

        @Test
        @DisplayName("만료된 정지가 없으면 아무 회원 상태도 변경하지 않는다")
        void it_leaves_members_untouched_when_the_expired_list_is_empty() {
            when(suspendRepository.findAllBySuspendedUntilBefore(any(LocalDateTime.class))).thenReturn(List.of());

            scheduler.releaseSuspensions();

            verify(suspendRepository).findAllBySuspendedUntilBefore(any(LocalDateTime.class));
            verifyNoMoreInteractions(suspendRepository);
        }
    }

    private Member suspendedMember(String name) {
        return Member.builder()
                .name(name)
                .nickname(name)
                .phoneNumber("010-0000-" + name)
                .password("password")
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.SUSPENDED)
                .build();
    }

    private Suspend expiredSuspend(Member member) {
        return Suspend.builder()
                .member(member)
                .suspendedAt(LocalDateTime.of(2000, 1, 1, 0, 0))
                .suspendedDays(1)
                .suspendedUntil(LocalDateTime.of(2000, 1, 2, 0, 0))
                .build();
    }
}
