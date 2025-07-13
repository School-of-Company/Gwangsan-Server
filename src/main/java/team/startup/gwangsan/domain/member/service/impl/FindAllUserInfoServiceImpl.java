package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
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
    public List<FindAllUserInfoResponse> execute() {
        Member currentMember = memberUtil.getCurrentMember();

        MemberDetail currentDetail = memberDetailRepository.findById(currentMember.getId())
                .orElseThrow(NotFoundMemberException::new);

        MemberRole role = currentMember.getRole();

        List<MemberDetail> details;

        if (role == MemberRole.ROLE_HEAD_ADMIN) {
            Integer headId = currentDetail.getPlace().getHead().getId();
            details = memberDetailRepository.findAllByPlace_Head_Id(headId);
        } else if (role == MemberRole.ROLE_PLACE_ADMIN) {
            Integer placeId = currentDetail.getPlace().getId();
            details = memberDetailRepository.findAllByPlace_Id(placeId);
        } else {
            throw new NotAllowedUserAccessException();
        }

        return details.stream()
                .map(detail -> {
                    Member member = detail.getMember();
                    return new FindAllUserInfoResponse(
                            member.getId(),
                            member.getNickname(),
                            member.getName(),
                            detail.getLight(),
                            detail.getGwangsan(),
                            member.getPhoneNumber(),
                            member.getRole(),
                            member.getStatus(),
                            member.getJoinedAt()
                    );
                })
                .toList();
    }
}
