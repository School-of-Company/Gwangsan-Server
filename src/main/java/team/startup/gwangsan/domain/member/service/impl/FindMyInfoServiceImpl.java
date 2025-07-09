package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindMyInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.service.FindMyInfoService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class FindMyInfoServiceImpl implements FindMyInfoService {

    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public FindMyInfoResponse execute() {
        Member member = memberUtil.getCurrentMember();

        MemberDetail detail = memberDetailRepository.findById(member.getId())
                .orElseThrow(NotFoundMemberException::new);

        return new FindMyInfoResponse(
                member.getId(),
                member.getNickname(),
                detail.getPlace().getName(),
                detail.getProfileUrl(),
                detail.getLight(),
                detail.getGwangsan(),
                detail.getDescription()
        );
    }
}