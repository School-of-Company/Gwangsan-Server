package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomProductDto;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomsResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.FindRoomsByCurrentUserService;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindRoomsByCurrentUserServiceImpl implements FindRoomsByCurrentUserService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetRoomsResponse> execute() {
        Long memberId = memberUtil.getCurrentMember().getId();
        List<GetRoomsDto> rooms = chatRoomRepository.findRoomsByMemberId(memberId);

        Set<Long> productIds = rooms.stream()
                .map(GetRoomsDto::productId)
                .collect(Collectors.toSet());

        Map<Long, GetRoomProductDto> productDtoMap = productRepository.findRoomProductsWithImagesByIds(productIds)
                .stream()
                .collect(Collectors.toMap(GetRoomProductDto::productId, Function.identity()));

        return rooms.stream()
                .map(room ->
                    new GetRoomsResponse(
                            room.roomId(),
                            room.member(),
                            room.messageId(),
                            room.lastMessage(),
                            room.lastMessageType(),
                            room.lastMessageTime(),
                            room.unreadMessageCount(),
                            productDtoMap.get(room.productId())
                    )
                )
                .toList();
    }
}
