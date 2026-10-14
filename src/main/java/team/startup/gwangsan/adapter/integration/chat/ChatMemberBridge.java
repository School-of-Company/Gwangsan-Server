package team.startup.gwangsan.adapter.integration.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.port.out.ChatMemberPort;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;

/** chat → member 공개 query. */
@Component
@RequiredArgsConstructor
public class ChatMemberBridge implements ChatMemberPort {

    private final MemberAccountQuery memberAccountQuery;

    @Override
    public boolean exists(Long memberId) {
        return memberAccountQuery.findById(memberId).isPresent();
    }
}
