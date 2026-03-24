package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.service.FindUserInfoService;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindUserInfoServiceImpl implements FindUserInfoService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;

    @Override
    @Transactional(readOnly = true)
    public FindUserInfoResponse execute(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        MemberDetail detail = memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        List<String> specialties = memberRelatedKeywordRepository.findAllByMember(member).stream()
                .map(mrk -> mrk.getRelatedKeyword().getName())
                .toList();

        return new FindUserInfoResponse(
                member.getId(),
                member.getNickname(),
                detail.getPlace().getName(),
                detail.getLight(),
                detail.getGwangsan(),
                detail.getDescription(),
                specialties,
                member.getName(),
                member.getPhoneNumber(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
    }
}
