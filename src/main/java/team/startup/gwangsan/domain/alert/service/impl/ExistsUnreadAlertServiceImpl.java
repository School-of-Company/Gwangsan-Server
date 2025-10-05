package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.presentation.dto.response.ExistsAlertResponse;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.service.ExistsUnreadAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class ExistsUnreadAlertServiceImpl implements ExistsUnreadAlertService {

    private final AlertReceiptRepository alertReceiptRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public ExistsAlertResponse execute() {
        Member member = memberUtil.getCurrentMember();

        return new ExistsAlertResponse(alertReceiptRepository.existsByMemberIdAndChecked(member.getId(), false));
    }
}
