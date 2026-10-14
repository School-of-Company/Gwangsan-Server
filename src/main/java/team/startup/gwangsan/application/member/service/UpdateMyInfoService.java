package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.UpdateMyInfoCommand;
import team.startup.gwangsan.application.member.port.in.UpdateMyInfoUseCase;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.MemberKeywordPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.exception.DuplicateMemberNicknameException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;

@Service
@RequiredArgsConstructor
public class UpdateMyInfoService implements UpdateMyInfoUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final MemberKeywordPort memberKeywordPort;

    @Override
    @Transactional
    public void execute(UpdateMyInfoCommand command) {
        Member member = loadMemberPort.findById(command.actor().memberId())
                .orElseThrow(NotFoundMemberException::new);
        if (!member.getNickname().equals(command.nickname())
                && loadMemberPort.existsByNickname(command.nickname())) {
            throw new DuplicateMemberNicknameException();
        }
        member.changeNickname(command.nickname());
        saveMemberPort.save(member);

        MemberDetail detail = loadMemberPort.findDetail(member.getId())
                .orElseThrow(NotFoundMemberException::new);
        detail.updateDescription(command.description());
        saveMemberPort.saveDetail(detail);

        memberKeywordPort.replace(member.getId(), command.specialties());
    }
}
