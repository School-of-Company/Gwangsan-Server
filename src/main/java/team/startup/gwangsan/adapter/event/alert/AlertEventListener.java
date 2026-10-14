package team.startup.gwangsan.adapter.event.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.alert.data.CreateAlertForMembersCommand;
import team.startup.gwangsan.application.alert.data.CreateAlertFromSourceCommand;
import team.startup.gwangsan.application.alert.port.in.CreateAlertFromSourceUseCase;

/**
 * 커밋 후 비동기 알림 저장 진입점. 가입·신고·정지·후기·공지 등은 이 경로를 계속 쓴다.
 * OTHER_MEMBER_TRADE_COMPLETE·TRADE_CANCEL 은 PR07 에서 RecordAlertUseCase 동기 저장으로 옮기며,
 * 그때 해당 발행자는 이 이벤트 발행을 지워 같은 알림이 두 번 저장되지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class AlertEventListener {

    private final CreateAlertFromSourceUseCase createAlertFromSourceUseCase;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreateAlertEvent(CreateAlertEvent event) {
        createAlertFromSourceUseCase.create(
                new CreateAlertFromSourceCommand(event.alertType(), event.sourceId(), event.memberId(), event.suspendId()));
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreateAlertMembersEvent(CreateAlertMembersEvent event) {
        createAlertFromSourceUseCase.createForMembers(
                new CreateAlertForMembersCommand(event.alertType(), event.sourceId(), event.memberIds()));
    }
}
