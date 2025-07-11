package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.DuplicateNicknameException;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.request.UpdateMyInfoRequest;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.service.UpdateMyInfoService;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateMyInfoServiceImpl implements UpdateMyInfoService {

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final RelatedKeywordRepository relatedKeywordRepository;
    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;

    @Override
    @Transactional
    public void execute(UpdateMyInfoRequest request) {
        Member member = memberUtil.getCurrentMember();

        if (!member.getNickname().equals(request.nickname())
                && memberRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
        }

        member.updateNickname(request.nickname());

        MemberDetail detail = memberDetailRepository.findById(member.getId())
                .orElseThrow(NotFoundMemberException::new);

        detail.updateDescription(request.description());

        memberRelatedKeywordRepository.deleteAllByMember(member);

        List<MemberRelatedKeyword> newKeywords = request.specialties().stream()
                .map(specialty -> {
                    RelatedKeyword keyword = relatedKeywordRepository.findByName(specialty)
                            .orElseGet(() -> relatedKeywordRepository.save(new RelatedKeyword(specialty)));
                    return new MemberRelatedKeyword(keyword, member);
                })
                .toList();

        memberRelatedKeywordRepository.saveAll(newKeywords);
    }
}
