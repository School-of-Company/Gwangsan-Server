package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomProductDto;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomsResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.FindRoomsByCurrentUserService;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindRoomsByCurrentUserServiceImpl implements FindRoomsByCurrentUserService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetRoomsResponse> execute() {
        Long memberId = memberUtil.getCurrentMember().getId();
        List<GetRoomsDto> rooms = chatRoomRepository.findRoomsByMemberId(memberId);

        Set<Long> productIds = rooms.stream()
                .map(GetRoomsDto::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ProductImage> productImages = productImageRepository.findAllByProductIdIn(productIds);

        Map<Long, List<GetImageResponse>> imageMap = productImages.stream()
                .collect(Collectors.groupingBy(
                        pi -> pi.getProduct().getId(),
                        Collectors.mapping(
                                pi -> new GetImageResponse(pi.getImage().getId(), pi.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        return rooms.stream()
                .map(room -> {
                    Product product = productMap.get(room.productId());
                    GetRoomProductDto productDto = null;

                    if (product != null) {
                        List<GetImageResponse> imageResponses = imageMap.getOrDefault(product.getId(), List.of());

                        productDto = new GetRoomProductDto(
                                product.getId(),
                                product.getTitle(),
                                imageResponses
                        );
                    }
                    return new GetRoomsResponse(
                            room.roomId(),
                            room.member(),
                            room.messageId(),
                            room.lastMessage(),
                            room.lastMessageType(),
                            room.lastMessageTime(),
                            room.unreadMessageCount(),
                            productDto
                    );
                })
                .toList();
    }
}
