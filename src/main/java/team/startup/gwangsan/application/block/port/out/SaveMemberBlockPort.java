package team.startup.gwangsan.application.block.port.out;

import team.startup.gwangsan.domain.block.model.MemberBlock;

public interface SaveMemberBlockPort {

    void save(MemberBlock memberBlock);

    /** @return 삭제한 차단이 있었으면 true */
    boolean delete(Long blockerId, Long blockedId);
}
