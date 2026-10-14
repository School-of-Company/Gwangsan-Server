package team.startup.gwangsan.application.suspend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.suspend.port.in.DeleteMemberSuspensionsUseCase;
import team.startup.gwangsan.application.suspend.port.out.SaveSuspensionPort;

@Service
@RequiredArgsConstructor
public class DeleteMemberSuspensionsService implements DeleteMemberSuspensionsUseCase {

    private final SaveSuspensionPort saveSuspensionPort;

    // 회원 탈퇴 트랜잭션에 참여한다(REQUIRED). 별도 커밋하지 않는다.
    @Override
    @Transactional
    public void deleteAllOf(Long memberId) {
        saveSuspensionPort.deleteAllOf(memberId);
    }
}
