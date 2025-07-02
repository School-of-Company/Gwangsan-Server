package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.GetMyInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.service.GetMyInfoService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class GetMyInfoServiceImpl implements GetMyInfoService {

    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public GetMyInfoResponse execute() {
        Member member = memberUtil.getCurrentMember();

        MemberDetail detail = memberDetailRepository.findById(member.getId())
                .orElseThrow(NotFoundMemberException::new);

        return new GetMyInfoResponse(
                member.getId(),
                member.getNickname(),
                detail.getProfileUrl(),
                detail.getLight(),
                detail.getGwangsan()
        );
    }
}