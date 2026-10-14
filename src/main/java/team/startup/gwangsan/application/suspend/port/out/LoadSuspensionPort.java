package team.startup.gwangsan.application.suspend.port.out;

import java.util.Optional;

public interface LoadSuspensionPort {
    Optional<Integer> findSuspendedDays(Long suspendId);
}
