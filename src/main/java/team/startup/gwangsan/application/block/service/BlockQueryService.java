package team.startup.gwangsan.application.block.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.block.data.BlockedMember;
import team.startup.gwangsan.application.block.port.in.BlockListQuery;
import team.startup.gwangsan.application.block.port.in.BlockRelationQuery;
import team.startup.gwangsan.application.block.port.out.LoadMemberBlockPort;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockQueryService implements BlockListQuery, BlockRelationQuery {

    private final LoadMemberBlockPort loadMemberBlockPort;

    @Override
    @Transactional(readOnly = true)
    public List<BlockedMember> query(Long actorId) {
        return loadMemberBlockPort.findBlockedMembers(actorId);
    }

    // 기존 BlockValidator 와 같이 트랜잭션을 열지 않는다. 호출자 트랜잭션이 있으면 그 안에서 조회한다.
    @Override
    public void validateNotBlocked(Long memberId, Long otherMemberId) {
        if (loadMemberBlockPort.existsBetween(memberId, otherMemberId)) {
            throw new BlockedMemberException();
        }
    }
}
