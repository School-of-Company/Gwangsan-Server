package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.UpdateMyPlaceCommand;
import team.startup.gwangsan.application.member.port.in.UpdateMyPlaceUseCase;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.MemberPlacePort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.MemberDetail;

@Service
@RequiredArgsConstructor
public class UpdateMyPlaceService implements UpdateMyPlaceUseCase {
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final MemberPlacePort memberPlacePort;

    @Override
    @Transactional
    public void execute(UpdateMyPlaceCommand command) {
        MemberDetail detail = loadMemberPort.findDetail(command.actor().memberId())
                .orElseThrow(NotFoundMemberDetailException::new);
        memberPlacePort.validate(command.placeId());
        detail.changePlace(command.placeId());
        saveMemberPort.saveDetail(detail);
    }
}
