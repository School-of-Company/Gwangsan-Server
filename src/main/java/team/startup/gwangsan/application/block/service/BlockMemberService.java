package team.startup.gwangsan.application.block.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.block.data.MemberBlockCommand;
import team.startup.gwangsan.application.block.port.in.BlockMemberUseCase;
import team.startup.gwangsan.application.block.port.out.LoadBlockTargetPort;
import team.startup.gwangsan.application.block.port.out.LoadMemberBlockPort;
import team.startup.gwangsan.application.block.port.out.SaveMemberBlockPort;
import team.startup.gwangsan.domain.block.exception.AlreadyBlockedException;
import team.startup.gwangsan.domain.block.model.MemberBlock;

@Service
@RequiredArgsConstructor
public class BlockMemberService implements BlockMemberUseCase {

    private final LoadMemberBlockPort loadMemberBlockPort;
    private final SaveMemberBlockPort saveMemberBlockPort;
    private final LoadBlockTargetPort loadBlockTargetPort;

    @Override
    @Transactional
    public void execute(MemberBlockCommand command) {
        MemberBlock block = MemberBlock.of(command.actorId(), command.targetMemberId());
        if (loadMemberBlockPort.exists(block.blockerId(), block.blockedId())) {
            throw new AlreadyBlockedException();
        }
        loadBlockTargetPort.checkMemberExists(block.blockedId());
        saveMemberBlockPort.save(block);
    }
}
