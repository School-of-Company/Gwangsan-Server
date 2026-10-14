package team.startup.gwangsan.application.block.port.in;

import team.startup.gwangsan.application.block.data.MemberBlockCommand;

public interface UnblockMemberUseCase {
    void execute(MemberBlockCommand command);
}
