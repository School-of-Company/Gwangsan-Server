package team.startup.gwangsan.application.report.port.out;

/** 신고 대상 확인. 대상이 없으면 소유 업무의 NotFound 예외(회원/게시글)를 그대로 던진다. */
public interface LoadReportTargetPort {

    /** 삭제되지 않은 게시글의 작성자 회원 ID. */
    Long loadActiveProductOwnerId(Long productId);

    void checkMemberExists(Long memberId);
}
