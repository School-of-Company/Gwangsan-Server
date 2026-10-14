package team.startup.gwangsan.application.relatedkeyword.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteAllMemberRelatedKeywordsUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.out.RelatedKeywordPort;

@Service
@RequiredArgsConstructor
public class DeleteAllMemberRelatedKeywordsService implements DeleteAllMemberRelatedKeywordsUseCase {

    private final RelatedKeywordPort relatedKeywordPort;

    @Override
    @Transactional
    public void execute(Long memberId) {
        relatedKeywordPort.bulkDeleteAllByMemberId(memberId);
    }
}
