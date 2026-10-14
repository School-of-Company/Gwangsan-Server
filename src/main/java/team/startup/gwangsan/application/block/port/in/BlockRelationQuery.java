package team.startup.gwangsan.application.block.port.in;

/** 다른 업무가 쓰는 차단 공개 계약. 어느 한쪽이라도 상대를 차단했으면 차단 관계다. */
public interface BlockRelationQuery {

    /** @throws team.startup.gwangsan.domain.block.exception.BlockedMemberException 차단 관계가 있으면 */
    void validateNotBlocked(Long memberId, Long otherMemberId);
}
