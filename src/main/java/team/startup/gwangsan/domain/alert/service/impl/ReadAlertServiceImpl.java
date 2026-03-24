package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.service.ReadAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadAlertServiceImpl implements ReadAlertService {

    private final AlertReceiptRepository alertReceiptRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long alertId) {
        Member member = memberUtil.getCurrentMember();
        List<AlertReceipt> alertReceipts = alertReceiptRepository.findByMemberIdAndCheckedAndAlertId(member.getId(), false, alertId);

        for (AlertReceipt alert : alertReceipts) {
            alert.markChecked();
        }
    }
}
