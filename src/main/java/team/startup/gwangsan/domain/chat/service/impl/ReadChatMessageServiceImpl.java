package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.service.ReadChatMessageService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
@DynamicUpdate
public class ReadChatMessageServiceImpl implements ReadChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long roomId, Long lastMessage) {
        Long readerId = memberUtil.getCurrentMember().getId();
        chatMessageRepository.readMessage(roomId, lastMessage, readerId);
    }
}
