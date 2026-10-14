package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.alert.service.CreateAlertMembersService;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateAlertMembersServiceImpl implements CreateAlertMembersService {

    private final AlertRepository alertRepository;
    private final MemberJpaRepository memberRepository;
    private final NoticeRepository noticeRepository;
    private final AlertReceiptRepository alertReceiptRepository;

    private static final String NOTICE_CONTENT = "공지가 등록되었습니다.";

    @Override
    @Transactional
    public void execute(Long sourceId, List<Long> memberIds, AlertType alertType) {
        List<MemberJpaEntity> members = memberRepository.findAllByIdIn(memberIds);

        if (members.isEmpty()) {
            return;
        }

        if (alertType == AlertType.NOTICE) {
            Notice notice = noticeRepository.findById(sourceId)
                    .orElseThrow(NoticeNotFoundException::new);

            saveAlert(sourceId, alertType, notice.getTitle(), NOTICE_CONTENT, members);
        }
    }

    private Alert createAlert(Long sourceId, AlertType alertType, String title, String content) {
        return Alert.builder()
                .alertType(alertType)
                .sourceId(sourceId)
                .title(title)
                .content(content)
                .build();
    }

    private AlertReceipt createAlertReceipt(Alert alert, MemberJpaEntity member) {
        return AlertReceipt.builder()
                .alert(alert)
                .member(member)
                .checked(false)
                .build();
    }

    private void saveAlert(Long sourceId, AlertType alertType, String title, String content, List<MemberJpaEntity> members) {
        Alert alert = alertRepository.save(createAlert(sourceId, alertType, title, content));

        List<AlertReceipt> alertReceipts = new ArrayList<>();

        for (MemberJpaEntity member : members) {
            alertReceipts.add(createAlertReceipt(alert, member));
        }

        alertReceiptRepository.saveAll(alertReceipts);
    }
}
