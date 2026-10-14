package team.startup.gwangsan.adapter.integration.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.global.util.BlockValidator;

/** chat → block legacy bridge (BlockedMemberException 403 유지). PR09 의 block 공개 query 로 교체. */
@Component
@RequiredArgsConstructor
public class ChatBlockBridge implements ChatBlockPort {

    private final BlockValidator blockValidator;

    @Override
    public void validateNotBlocked(Long memberId, Long otherMemberId) {
        blockValidator.validate(memberId, otherMemberId);
    }
}
