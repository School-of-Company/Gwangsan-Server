package team.startup.gwangsan.application.suspend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.suspend.port.in.SuspensionQuery;
import team.startup.gwangsan.application.suspend.port.out.LoadSuspensionPort;
import team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException;

@Service
@RequiredArgsConstructor
public class SuspensionQueryService implements SuspensionQuery {

    private final LoadSuspensionPort loadSuspensionPort;

    @Override
    @Transactional(readOnly = true)
    public int getSuspendedDays(Long suspendId) {
        if (suspendId == null) {
            throw new NotFoundSuspendException();
        }
        return loadSuspensionPort.findSuspendedDays(suspendId).orElseThrow(NotFoundSuspendException::new);
    }
}
