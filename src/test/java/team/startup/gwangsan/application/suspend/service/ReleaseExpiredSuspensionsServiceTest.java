package team.startup.gwangsan.application.suspend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.suspend.port.out.ChangeMemberStatusPort;
import team.startup.gwangsan.application.suspend.port.out.LoadExpiredSuspensionPort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseExpiredSuspensionsService 단위 테스트")
class ReleaseExpiredSuspensionsServiceTest {

    @Mock private LoadExpiredSuspensionPort loadExpiredSuspensionPort;
    @Mock private ChangeMemberStatusPort changeMemberStatusPort;
    @InjectMocks private ReleaseExpiredSuspensionsService service;

    private final LocalDateTime now = LocalDateTime.of(2026, 10, 14, 10, 0);

    @Test
    @DisplayName("it_해제 대상 회원마다 ACTIVE 로 바꾸고 해제한 회원 수를 반환한다")
    void it_activates_each_releasable_member() {
        when(loadExpiredSuspensionPort.findReleasableMemberIds(now)).thenReturn(List.of(1L, 2L));

        int released = service.execute(now);

        assertThat(released).isEqualTo(2);
        verify(changeMemberStatusPort).activate(1L);
        verify(changeMemberStatusPort).activate(2L);
        verifyNoMoreInteractions(changeMemberStatusPort);
    }

    @Test
    @DisplayName("it_만료 정지가 없으면 회원 상태를 바꾸지 않는다")
    void it_leaves_members_untouched() {
        when(loadExpiredSuspensionPort.findReleasableMemberIds(now)).thenReturn(List.of());

        assertThat(service.execute(now)).isZero();
        verifyNoInteractions(changeMemberStatusPort);
    }
}
