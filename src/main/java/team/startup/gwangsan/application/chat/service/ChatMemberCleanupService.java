package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.port.in.ChatMemberCleanupUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatMemberCleanupPort;

@Service
@RequiredArgsConstructor
public class ChatMemberCleanupService implements ChatMemberCleanupUseCase {

    private final ChatMemberCleanupPort chatMemberCleanupPort;

    @Override
    @Transactional
    public void reassignMember(Long memberId, Long dummyMemberId) {
        chatMemberCleanupPort.reassignMember(memberId, dummyMemberId);
    }
}
