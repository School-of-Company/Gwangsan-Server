package team.startup.gwangsan.application.relatedkeyword.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.relatedkeyword.data.DeleteMemberRelatedKeywordCommand;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteMemberRelatedKeywordUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.out.RelatedKeywordPort;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordNotFoundException;
import team.startup.gwangsan.domain.relatedkeyword.model.MemberKeyword;

@Service
@RequiredArgsConstructor
public class DeleteMemberRelatedKeywordService implements DeleteMemberRelatedKeywordUseCase {

    private final RelatedKeywordPort relatedKeywordPort;

    @Override
    @Transactional
    public void execute(DeleteMemberRelatedKeywordCommand command) {
        MemberKeyword memberKeyword = relatedKeywordPort.findMemberKeyword(command.memberRelatedKeywordId())
                .orElseThrow(MemberRelatedKeywordNotFoundException::new);
        memberKeyword.checkOwnedBy(command.actorMemberId());
        relatedKeywordPort.deleteMemberKeyword(memberKeyword.id());
    }
}
