package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.port.in.ChangePasswordUseCase;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.Member;

@Service
@RequiredArgsConstructor
public class ChangePasswordService implements ChangePasswordUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    @Override
    @Transactional
    public void execute(Long memberId, String passwordHash) {
        Member member = loadMemberPort.findById(memberId).orElseThrow(NotFoundMemberException::new);
        member.changePassword(passwordHash);
        saveMemberPort.save(member);
    }
}
