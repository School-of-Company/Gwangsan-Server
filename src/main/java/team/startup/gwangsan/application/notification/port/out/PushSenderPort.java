package team.startup.gwangsan.application.notification.port.out;

import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.domain.notification.model.PushOutcome;

import java.util.List;

public interface PushSenderPort {
    /**
     * 일시 실패만 제한 재시도하고 영구 실패는 재시도하지 않는다. 최종 실패는 FAILED 로 돌려준다.
     */
    PushOutcome send(PushMessage message, List<String> tokens);
}
