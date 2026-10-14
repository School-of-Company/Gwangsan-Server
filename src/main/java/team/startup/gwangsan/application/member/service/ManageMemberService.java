package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.port.in.ManageMemberUseCase;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.util.Optional;

/** 정지·승인·역할 변경은 호출자 업무(정지 이력·알림 처리)와 함께 커밋돼야 하므로 MANDATORY 다. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ManageMemberService implements ManageMemberUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    @Override
    public void changeStatus(Long memberId, MemberStatus status) {
        Member member = member(memberId);
        member.changeStatus(status);
        saveMemberPort.save(member);
    }

    @Override
    public void changeRole(Long memberId, MemberRole role, Integer placeId) {
        // 순서는 기존과 같다: 상세(지점) → 회원(역할).
        if (placeId != null) {
            MemberDetail detail = detail(memberId);
            detail.changePlace(placeId);
            saveMemberPort.saveDetail(detail);
        }
        Member member = member(memberId);
        member.changeRole(role);
        saveMemberPort.save(member);
    }

    @Override
    public void rejectSignUp(Long memberId) {
        detail(memberId);
        saveMemberPort.deleteDetail(memberId);
    }

    @Override
    public Optional<Integer> takeWithdrawnGwangsan(String phoneNumber) {
        return saveMemberPort.removeWithdrawalRecord(phoneNumber);
    }

    private Member member(Long memberId) {
        return loadMemberPort.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }

    private MemberDetail detail(Long memberId) {
        return loadMemberPort.findDetail(memberId).orElseThrow(NotFoundMemberDetailException::new);
    }
}
