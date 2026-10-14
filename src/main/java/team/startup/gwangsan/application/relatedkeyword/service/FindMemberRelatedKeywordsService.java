package team.startup.gwangsan.application.relatedkeyword.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.application.relatedkeyword.port.in.FindMemberRelatedKeywordsQuery;
import team.startup.gwangsan.application.relatedkeyword.port.out.RelatedKeywordPort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindMemberRelatedKeywordsService implements FindMemberRelatedKeywordsQuery {

    private final RelatedKeywordPort relatedKeywordPort;

    @Override
    @Transactional(readOnly = true)
    public List<RelatedKeywordResult> query(Long memberId) {
        return relatedKeywordPort.findAllByMemberId(memberId);
    }
}
