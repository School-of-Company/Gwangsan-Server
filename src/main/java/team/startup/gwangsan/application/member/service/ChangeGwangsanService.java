package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.MemberDetail;

import java.util.function.Consumer;

/**
 * 잔액은 거래 상태·환불·알림과 함께 커밋돼야 하므로 호출자 트랜잭션을 필수로 요구한다(MANDATORY).
 * 단독 호출은 거절되어 잔액만 따로 커밋되는 경로를 만들지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ChangeGwangsanService implements ChangeGwangsanUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    @Override
    public void plus(Long memberId, int amount) {
        change(memberId, detail -> detail.plusGwangsan(amount));
    }

    @Override
    public void minus(Long memberId, int amount) {
        change(memberId, detail -> detail.minusGwangsan(amount));
    }

    @Override
    public void adjust(Long memberId, int delta) {
        change(memberId, detail -> detail.adjustGwangsan(delta));
    }

    private void change(Long memberId, Consumer<MemberDetail> change) {
        MemberDetail detail = loadMemberPort.findDetail(memberId).orElseThrow(NotFoundMemberDetailException::new);
        change.accept(detail);
        saveMemberPort.saveDetail(detail);
    }
}
