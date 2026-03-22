package team.startup.gwangsan.domain.alert.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAlertMembersServiceImpl 단위 테스트")
class CreateAlertMembersServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private AlertReceiptRepository alertReceiptRepository;

    @InjectMocks
    private CreateAlertMembersServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("해당하는 회원이 없으면 저장하지 않고 바로 반환한다")
        void it_returns_early_when_no_members() {
            when(memberRepository.findAllByIdIn(anyList())).thenReturn(List.of());

            service.execute(1L, List.of(10L, 20L), AlertType.NOTICE);

            verifyNoInteractions(alertRepository, alertReceiptRepository);
        }

        @Test
        @DisplayName("NOTICE 타입이면 공지를 조회하고 모든 회원에게 AlertReceipt 를 저장한다")
        void it_saves_alert_and_receipts_for_notice() {
            Member member1 = mock(Member.class);
            Member member2 = mock(Member.class);
            when(memberRepository.findAllByIdIn(anyList())).thenReturn(List.of(member1, member2));

            Notice notice = mock(Notice.class);
            when(notice.getTitle()).thenReturn("공지 제목");
            when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(1L, List.of(10L, 20L), AlertType.NOTICE);

            ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(alertCaptor.capture());
            assertThat(alertCaptor.getValue().getTitle()).isEqualTo("공지 제목");
            assertThat(alertCaptor.getValue().getContent()).isEqualTo("공지가 등록되었습니다.");

            ArgumentCaptor<List> receiptCaptor = ArgumentCaptor.forClass(List.class);
            verify(alertReceiptRepository).saveAll(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("NOTICE 타입인데 공지가 없으면 NoticeNotFoundException 을 던진다")
        void it_throws_NoticeNotFoundException_when_notice_not_found() {
            Member member = mock(Member.class);
            when(memberRepository.findAllByIdIn(anyList())).thenReturn(List.of(member));
            when(noticeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NoticeNotFoundException.class,
                    () -> service.execute(99L, List.of(10L), AlertType.NOTICE));

            verifyNoInteractions(alertRepository, alertReceiptRepository);
        }

        @Test
        @DisplayName("NOTICE 외 타입이면 알림을 저장하지 않는다")
        void it_does_nothing_for_non_notice_alert_type() {
            Member member = mock(Member.class);
            when(memberRepository.findAllByIdIn(anyList())).thenReturn(List.of(member));

            service.execute(1L, List.of(10L), AlertType.REVIEW);

            verifyNoInteractions(alertRepository, alertReceiptRepository, noticeRepository);
        }
    }
}
