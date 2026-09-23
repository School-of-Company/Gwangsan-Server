package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.ValidateChatSendableResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.ValidateChatSendableService;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.InvalidImageIdsException;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.ImageValidateUtil;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ValidateChatSendableServiceImpl implements ValidateChatSendableService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final BlockValidator blockValidator;
    private final ImageRepository imageRepository;

    @Override
    @Transactional(readOnly = true)
    public ValidateChatSendableResponse execute(Long roomId, List<String> imageIds) {
        Member member = memberUtil.getCurrentMember();
        ChatRoom chatRoom = chatRoomRepository.findChatRoomByRoomId(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        // 참여자가 아니면 방의 존재 자체를 숨긴다 (조회 API 와 동일 정책).
        if (!chatRoom.isParticipant(member)) {
            throw new NotFoundChatRoomException();
        }

        blockValidator.validate(member, chatRoom.getOtherMember(member));

        if (imageIds == null) {
            return null;
        }
        if (imageIds.isEmpty()) {
            throw new InvalidImageIdsException();
        }

        List<Long> parsedIds = imageIds.stream().map(this::parseImageId).toList();
        List<Image> images = imageRepository.findAllById(parsedIds);
        ImageValidateUtil.validateExistence(parsedIds, images);
        Map<Long, Image> imagesById = images.stream()
                .collect(Collectors.toMap(Image::getId, Function.identity()));
        return new ValidateChatSendableResponse(parsedIds.stream()
                .map(id -> new GetImageResponse(id, imagesById.get(id).getImageUrl()))
                .toList());
    }

    private Long parseImageId(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw new InvalidImageIdsException();
        }
        try {
            long id = Long.parseLong(value);
            if (id <= 0 || id > 9_007_199_254_740_991L) {
                throw new InvalidImageIdsException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new InvalidImageIdsException();
        }
    }
}
