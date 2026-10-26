package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

/** 같은 (사유, 기준 ID, 회원) 의 광산 이동이 이미 원장에 있다. 트랜잭션 전체가 롤백된다. */
public class DuplicateGwangsanLedgerException extends GlobalException {
    public DuplicateGwangsanLedgerException() {
        super(ErrorCode.DUPLICATE_GWANGSAN_LEDGER);
    }
}
