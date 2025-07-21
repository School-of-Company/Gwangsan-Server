package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.presentation.dto.response.CreateChatRoomResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.CreateChatRoomService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class CreateChatRoomServiceImpl implements CreateChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public CreateChatRoomResponse execute(Long productId) {
        Member member = memberUtil.getCurrentMember();

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
        Member productMember = product.getMember();

        Member member1 = member.getId() < productMember.getId() ? member : productMember;
        Member member2 = member.getId() < productMember.getId() ? productMember : member;


        ChatRoom existsChatRoom = chatRoomRepository.findByProductIdAndMember1AndMember2(productId, member1, member2)
                .orElse(null);
        if (existsChatRoom != null) {
            return new CreateChatRoomResponse(existsChatRoom.getId());
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .member1(member1)
                .member2(member2)
                .isActive(true)
                .product(product)
                .build();

        return new CreateChatRoomResponse(chatRoomRepository.save(chatRoom).getId());
    }
}
