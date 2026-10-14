package team.startup.gwangsan.application.suspend.port.out;

import team.startup.gwangsan.domain.suspend.model.Suspension;

public interface SaveSuspensionPort {

    /** @return 저장된 정지 ID */
    Long save(Suspension suspension);
}
