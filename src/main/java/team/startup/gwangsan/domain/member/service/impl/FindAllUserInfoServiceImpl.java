package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotAllowedUserAccessException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.projection.FindAllUserDto;
import team.startup.gwangsan.domain.member.service.FindAllUserInfoService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class FindAllUserInfoServiceImpl implements FindAllUserInfoService {

    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public Slice<FindAllUserInfoResponse> execute(String nickname, String placeName, Pageable pageable) {
        Member currentMember = memberUtil.getCurrentMember();

        MemberDetail currentDetail = memberDetailRepository.findById(currentMember.getId())
                .orElseThrow(NotFoundMemberException::new);

        Integer placeId = null;
        Integer headId = null;

        switch (currentMember.getRole()) {
            case ROLE_HEAD_ADMIN -> headId = currentDetail.getPlace().getHead().getId();
            case ROLE_PLACE_ADMIN -> placeId = currentDetail.getPlace().getId();
            default -> throw new NotAllowedUserAccessException();
        }

        return memberDetailRepository
                .findAllUserSlice(placeId, headId, nickname, placeName, pageable)
                .map(this::toResponse);
    }

    private FindAllUserInfoResponse toResponse(FindAllUserDto dto) {
        return new FindAllUserInfoResponse(
                dto.memberId(),
                dto.nickname(),
                dto.name(),
                dto.phoneNumber(),
                dto.role(),
                dto.status(),
                dto.joinedAt(),
                dto.gwangsan()
        );
    }
}