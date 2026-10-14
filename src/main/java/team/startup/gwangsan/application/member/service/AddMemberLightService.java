package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.port.in.AddMemberLightUseCase;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.MemberDetail;

/** 빛은 후기와 함께 커밋돼야 하므로 호출자 트랜잭션을 필수로 요구한다(MANDATORY). */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AddMemberLightService implements AddMemberLightUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    @Override
    public void execute(Long memberId, Integer delta) {
        MemberDetail detail = loadMemberPort.findDetail(memberId).orElseThrow(NotFoundMemberDetailException::new);
        detail.plusLight(delta);
        saveMemberPort.saveDetail(detail);
    }
}
