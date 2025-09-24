package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotAllowedUserAccessException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.service.FindAllUserInfoService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAllUserInfoServiceImpl implements FindAllUserInfoService {

    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<FindAllUserInfoResponse> execute(String nickname, String placeName) {
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

        List<MemberDetail> details = memberDetailRepository.findAllByRoleAndNicknameAndPlaceName(
                placeId, headId, nickname, placeName
        );

        return details.stream()
                .map(detail -> {
                    Member member = detail.getMember();
                    return new FindAllUserInfoResponse(
                            member.getId(),
                            member.getNickname(),
                            member.getName(),
                            member.getPhoneNumber(),
                            member.getRole(),
                            member.getStatus(),
                            member.getJoinedAt()
                    );
                })
                .toList();
    }
}