package team.startup.gwangsan.application.block.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.block.port.in.DeleteMemberBlocksUseCase;
import team.startup.gwangsan.application.block.port.out.SaveMemberBlockPort;

@Service
@RequiredArgsConstructor
public class DeleteMemberBlocksService implements DeleteMemberBlocksUseCase {

    private final SaveMemberBlockPort saveMemberBlockPort;

    // 회원 탈퇴 트랜잭션에 참여한다(REQUIRED). 별도 커밋하지 않는다.
    @Override
    @Transactional
    public void deleteAllOf(Long memberId) {
        saveMemberBlockPort.deleteAllOf(memberId);
    }
}
