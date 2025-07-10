package team.startup.gwangsan.domain.relatedkeyword.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.presentation.dto.response.RelatedKeywordResponse;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.service.FindMyRelatedKeywordService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindMyRelatedKeywordServiceImpl implements FindMyRelatedKeywordService {

    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<RelatedKeywordResponse> execute() {
        Member member = memberUtil.getCurrentMember();

        List<MemberRelatedKeyword> list = memberRelatedKeywordRepository.findAllByMember(member);

        return list.stream()
                .map(mrk -> new RelatedKeywordResponse(
                        mrk.getRelatedKeyword().getId(),
                        mrk.getRelatedKeyword().getName()
                ))
                .collect(Collectors.toList());
    }
}
