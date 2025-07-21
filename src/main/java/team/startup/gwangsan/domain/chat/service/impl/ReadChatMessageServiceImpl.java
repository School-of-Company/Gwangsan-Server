package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
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
public class ReadChatMessageServiceImpl implements ReadChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long roomId, Long lastMessage) {
        Member member = memberUtil.getCurrentMember();
        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessages(roomId, lastMessage, member.getId());

        for (ChatMessage chatMessage : unreadMessages) {
            chatMessage.updateChecked(true);
        }

        chatMessageRepository.saveAll(unreadMessages);
    }
}
