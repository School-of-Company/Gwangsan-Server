package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.alert.presentation.dto.response.ExistsAlertResponse;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.alert.service.ExistsUnreadAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class ExistsUnreadAlertServiceImpl implements ExistsUnreadAlertService {

    private final AlertRepository alertRepository;
    private final MemberUtil memberUtil;

    @Override
    public ExistsAlertResponse execute() {
        Member member = memberUtil.getCurrentMember();

        return new ExistsAlertResponse(alertRepository.existsByMemberAndChecked(member, false));
    }
}
