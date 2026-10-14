package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.RegisterMemberCommand;
import team.startup.gwangsan.application.member.port.in.RegisterMemberUseCase;
import team.startup.gwangsan.application.member.port.out.MemberKeywordPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;

@Service
@RequiredArgsConstructor
public class RegisterMemberService implements RegisterMemberUseCase {

    private final SaveMemberPort saveMemberPort;
    private final MemberKeywordPort memberKeywordPort;

    @Override
    @Transactional
    public Long execute(RegisterMemberCommand command) {
        Member member = Member.applicant(command.name(), command.nickname(), command.passwordHash(),
                command.phoneNumber(), command.recommenderId());
        MemberDetail detail = MemberDetail.applicant(command.dongId(), command.placeId(), command.description());
        Long memberId = saveMemberPort.register(member, detail);
        memberKeywordPort.add(memberId, command.specialties());
        return memberId;
    }
}
