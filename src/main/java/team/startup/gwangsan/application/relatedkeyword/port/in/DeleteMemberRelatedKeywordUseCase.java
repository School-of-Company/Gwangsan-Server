package team.startup.gwangsan.application.relatedkeyword.port.in;

import team.startup.gwangsan.application.relatedkeyword.data.DeleteMemberRelatedKeywordCommand;

public interface DeleteMemberRelatedKeywordUseCase {
    void execute(DeleteMemberRelatedKeywordCommand command);
}
