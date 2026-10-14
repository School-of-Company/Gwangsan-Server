package team.startup.gwangsan.application.alert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.alert.port.in.ReadAlertsUseCase;
import team.startup.gwangsan.application.alert.port.out.MarkAlertsReadPort;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReadAlertsService implements ReadAlertsUseCase {

    private final MarkAlertsReadPort markAlertsReadPort;

    @Override
    @Transactional
    public void readUpTo(Long memberId, Long alertId) {
        // 기존 AlertReceipt.markChecked 와 같은 기본 timezone 의 현재 시각을 쓴다.
        markAlertsReadPort.markReadUpTo(memberId, alertId, LocalDateTime.now());
    }
}
