package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.presentation.dto.response.CreateChatRoomResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.CreateChatRoomService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.repository.custom.impl.ProductCustomRepositoryImpl;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class CreateChatRoomServiceImpl implements CreateChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ProductCustomRepositoryImpl productCustomRepositoryImpl;

    @Override
    public CreateChatRoomResponse execute(Long productId) {
        Member member = memberUtil.getCurrentMember();

        Product product = productCustomRepositoryImpl.findProductById(productId);
        Member productMember = product.getMember();

        Member member1;
        Member member2;
        if (member.getId() < productMember.getId()) {
            member1 = member;
            member2 = productMember;
        } else {
            member1 = productMember;
            member2 = member;
        }

        ChatRoom existsChatRoom = chatRoomRepository.findByMember1AndMember2(member1, member2)
                .orElse(null);
        if (existsChatRoom != null) {
            return new CreateChatRoomResponse(existsChatRoom.getId());
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .member1(member1)
                .member2(member2)
                .isActive(true)
                .build();

        return new CreateChatRoomResponse(chatRoomRepository.save(chatRoom).getId());
    }
}
