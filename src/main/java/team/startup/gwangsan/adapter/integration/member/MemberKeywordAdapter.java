package team.startup.gwangsan.adapter.integration.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.out.MemberKeywordPort;
import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.application.relatedkeyword.port.in.FindMemberRelatedKeywordsQuery;
import team.startup.gwangsan.application.relatedkeyword.port.in.MemberKeywordCommand;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberKeywordAdapter implements MemberKeywordPort {

    private final FindMemberRelatedKeywordsQuery findMemberRelatedKeywordsQuery;
    private final MemberKeywordCommand memberKeywordCommand;

    @Override
    public List<String> findNames(Long memberId) {
        return findMemberRelatedKeywordsQuery.query(memberId).stream()
                .map(RelatedKeywordResult::name)
                .toList();
    }

    @Override
    public void add(Long memberId, List<String> names) {
        memberKeywordCommand.add(memberId, names);
    }

    @Override
    public void replace(Long memberId, List<String> names) {
        memberKeywordCommand.replace(memberId, names);
    }
}
