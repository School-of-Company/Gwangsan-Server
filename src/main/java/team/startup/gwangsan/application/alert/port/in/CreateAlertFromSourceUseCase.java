package team.startup.gwangsan.application.alert.port.in;

import team.startup.gwangsan.application.alert.data.CreateAlertForMembersCommand;
import team.startup.gwangsan.application.alert.data.CreateAlertFromSourceCommand;

/**
 * 커밋 후 비동기 알림 저장. 원천 데이터를 조회해 유형별 알림을 만든다 (CreateAlertEvent/CreateAlertMembersEvent).
 * 업무 트랜잭션과 원자적이지 않다. 원자 저장이 필요한 두 유형은 RecordAlertUseCase 를 쓴다.
 */
public interface CreateAlertFromSourceUseCase {
    void create(CreateAlertFromSourceCommand command);

    void createForMembers(CreateAlertForMembersCommand command);
}
