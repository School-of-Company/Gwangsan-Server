package team.startup.gwangsan.application.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.application.notification.port.out.LoadPushTokensPort;
import team.startup.gwangsan.application.notification.port.out.PushSenderPort;
import team.startup.gwangsan.domain.notification.exception.PushDeliveryFailedException;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.domain.notification.model.PushOutcome;

// DB 를 쓰지 않으므로 트랜잭션을 열지 않는다. 호출자 트랜잭션 안에서 불려도 롤백 대상이 없다.
@Service
@RequiredArgsConstructor
public class SendPushService implements SendPushUseCase {

    private final LoadPushTokensPort loadPushTokensPort;
    private final PushSenderPort pushSenderPort;

    @Override
    public PushOutcome send(SendPushCommand command) {
        PushMessage message = command.message();
        PushOutcome outcome = pushSenderPort.send(message, loadPushTokensPort.loadTokens(command.recipientMemberIds()));
        if (outcome == PushOutcome.FAILED && message.type() != null && message.type().requiresDeliveryConfirmation()) {
            throw new PushDeliveryFailedException(message.type(), message.sourceId());
        }
        return outcome;
    }
}
