package team.startup.gwangsan.adapter.integration.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.application.block.port.in.BlockRelationQuery;

/** chat → block 공개 query(PR09) 연결. 차단이면 BlockedMemberException(403). */
@Component
@RequiredArgsConstructor
public class ChatBlockBridge implements ChatBlockPort {

    private final BlockRelationQuery blockRelationQuery;

    @Override
    public void validateNotBlocked(Long memberId, Long otherMemberId) {
        blockRelationQuery.validateNotBlocked(memberId, otherMemberId);
    }
}
