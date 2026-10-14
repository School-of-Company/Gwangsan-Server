package team.startup.gwangsan.application.alert.port.in;

import team.startup.gwangsan.application.alert.data.RecordAlertCommand;

/**
 * 중요한 업무 알림과 수신자 receipt 를 호출자 트랜잭션 안에서 저장한다.
 * 별도 트랜잭션·비동기 저장을 하지 않으므로 저장 실패 시 호출한 업무도 함께 rollback 된다.
 * 대상은 {@link team.startup.gwangsan.domain.alert.model.AlertType#isRecordedWithBusinessTransaction()} 유형뿐이며,
 * 그 밖의 유형은 UnsupportedAlertTypeException 으로 거절한다.
 */
public interface RecordAlertUseCase {
    Long record(RecordAlertCommand command);
}
