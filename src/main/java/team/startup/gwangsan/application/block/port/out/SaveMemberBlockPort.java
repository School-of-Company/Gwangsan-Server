package team.startup.gwangsan.application.block.port.out;

import team.startup.gwangsan.domain.block.model.MemberBlock;

public interface SaveMemberBlockPort {

    void save(MemberBlock memberBlock);

    /** @return 삭제한 차단이 있었으면 true */
    boolean delete(Long blockerId, Long blockedId);

    /** 차단자·피차단자 어느 쪽이든 회원이 포함된 행을 bulk 삭제한다(영속성 컨텍스트 자동 clear). */
    void deleteAllOf(Long memberId);
}
