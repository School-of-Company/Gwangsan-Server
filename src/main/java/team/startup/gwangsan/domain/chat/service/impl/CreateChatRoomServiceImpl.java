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
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateChatRoomServiceImpl implements CreateChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final BlockValidator blockValidator;

    @Override
    @Transactional
    public CreateChatRoomResponse execute(Long productId) {
        Member member = memberUtil.getCurrentMember();

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
        Member productMember = product.getMember();

        blockValidator.validate(member, productMember);

        Member buyer;
        Member seller;

        if (product.getMode() == Mode.GIVER) {
            buyer = member;
            seller = productMember;
        } else {
            buyer = productMember;
            seller = member;
        }

        Optional<ChatRoom> existsChatRoom = chatRoomRepository.findByProductIdAndBuyerAndSeller(productId, buyer, seller);

        if (existsChatRoom.isPresent()) {
            return new CreateChatRoomResponse(existsChatRoom.get().getId());
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .buyer(buyer)
                .seller(seller)
                .isActive(true)
                .product(product)
                .build();

        return new CreateChatRoomResponse(chatRoomRepository.save(chatRoom).getId());
    }
}
