package team.startup.gwangsan.domain.relatedkeyword.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordForbiddenException;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordNotFoundException;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.service.DeleteMemberRelatedKeywordService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class DeleteMemberRelatedKeywordServiceImpl implements DeleteMemberRelatedKeywordService {

    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long memberRelatedKeywordId) {
        Member member = memberUtil.getCurrentMember();

        MemberRelatedKeyword memberRelatedKeyword = memberRelatedKeywordRepository.findById(memberRelatedKeywordId)
                .orElseThrow(MemberRelatedKeywordNotFoundException::new);

        if (!memberRelatedKeyword.getMember().equals(member)) {
            throw new MemberRelatedKeywordForbiddenException();
        }

        memberRelatedKeywordRepository.delete(memberRelatedKeyword);
    }
}

