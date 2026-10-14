package team.startup.gwangsan.application.suspend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.suspend.port.in.ReleaseExpiredSuspensionsUseCase;
import team.startup.gwangsan.application.suspend.port.out.ChangeMemberStatusPort;
import team.startup.gwangsan.application.suspend.port.out.LoadExpiredSuspensionPort;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReleaseExpiredSuspensionsService implements ReleaseExpiredSuspensionsUseCase {

    private final LoadExpiredSuspensionPort loadExpiredSuspensionPort;
    private final ChangeMemberStatusPort changeMemberStatusPort;

    // 한 트랜잭션: 중간 회원 활성화가 실패하면 앞서 바꾼 회원까지 모두 rollback 한다.
    @Override
    @Transactional
    public int execute(LocalDateTime now) {
        List<Long> memberIds = loadExpiredSuspensionPort.findReleasableMemberIds(now);
        memberIds.forEach(changeMemberStatusPort::activate);
        return memberIds.size();
    }
}
