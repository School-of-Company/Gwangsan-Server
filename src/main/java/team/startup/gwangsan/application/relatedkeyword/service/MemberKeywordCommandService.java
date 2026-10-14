package team.startup.gwangsan.application.relatedkeyword.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.relatedkeyword.port.in.MemberKeywordCommand;
import team.startup.gwangsan.application.relatedkeyword.port.out.RelatedKeywordPort;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberKeywordCommandService implements MemberKeywordCommand {

    private final RelatedKeywordPort relatedKeywordPort;

    @Override
    public void add(Long memberId, List<String> names) {
        relatedKeywordPort.addMemberKeywords(memberId, names);
    }

    @Override
    public void replace(Long memberId, List<String> names) {
        relatedKeywordPort.replaceMemberKeywords(memberId, names);
    }
}
