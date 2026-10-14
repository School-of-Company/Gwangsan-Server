package team.startup.gwangsan.application.block.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.block.data.MemberBlockCommand;
import team.startup.gwangsan.application.block.port.in.UnblockMemberUseCase;
import team.startup.gwangsan.application.block.port.out.SaveMemberBlockPort;
import team.startup.gwangsan.domain.block.exception.NotFoundBlockException;

@Service
@RequiredArgsConstructor
public class UnblockMemberService implements UnblockMemberUseCase {

    private final SaveMemberBlockPort saveMemberBlockPort;

    @Override
    @Transactional
    public void execute(MemberBlockCommand command) {
        if (!saveMemberBlockPort.delete(command.actorId(), command.targetMemberId())) {
            throw new NotFoundBlockException();
        }
    }
}
