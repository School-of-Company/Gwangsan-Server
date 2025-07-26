package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.alert.service.CreateAlertMembersService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateAlertMembersServiceImpl implements CreateAlertMembersService {

    private final AlertRepository alertRepository;
    private final MemberRepository memberRepository;
    private final NoticeRepository noticeRepository;

    private static final String NOTICE_CONTENT = "공지가 등록되었습니다.";

    @Override
    public void execute(Long sourceId, List<Long> memberIds, AlertType alertType) {
        List<Member> members = memberRepository.findAllByIdIn(memberIds);

        if (alertType == AlertType.NOTICE) {
            Notice notice = noticeRepository.findById(sourceId)
                    .orElseThrow(NoticeNotFoundException::new);

            saveAlert(sourceId, alertType, members, notice.getTitle(), NOTICE_CONTENT);
        }
    }

    private Alert createAlert(Long sourceId, AlertType alertType, Member member, String title, String content) {
        return Alert.builder()
                .alertType(alertType)
                .sourceId(sourceId)
                .member(member)
                .title(title)
                .content(content)
                .checked(false)
                .build();
    }

    private void saveAlert(Long sourceId, AlertType alertType, List<Member> members, String title, String content) {
        List<Alert> alerts = new ArrayList<>();

        for (Member member : members) {
            alerts.add(createAlert(sourceId, alertType, member, title, content));
        }
        alertRepository.saveAll(alerts);
    }
}
