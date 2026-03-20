package team.startup.gwangsan.domain.alert.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadAlertServiceImpl 단위 테스트")
class ReadAlertServiceImplTest {

    @Mock
    private AlertReceiptRepository alertReceiptRepository;

    @Mock
    private MemberUtil memberUtil;

    @InjectMocks
    private ReadAlertServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("checked=false 인 AlertReceipt 들을 모두 markChecked() 한다")
        void it_marks_all_unchecked_receipts_as_checked() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            AlertReceipt receipt1 = mock(AlertReceipt.class);
            AlertReceipt receipt2 = mock(AlertReceipt.class);
            when(alertReceiptRepository.findByMemberIdAndCheckedAndAlertId(1L, false, 10L))
                    .thenReturn(List.of(receipt1, receipt2));

            service.execute(10L);

            verify(receipt1).markChecked();
            verify(receipt2).markChecked();
        }

        @Test
        @DisplayName("해당 알림에 읽지 않은 receipt 가 없으면 markChecked() 를 호출하지 않는다")
        void it_does_nothing_when_no_unchecked_receipts() {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);

            when(alertReceiptRepository.findByMemberIdAndCheckedAndAlertId(1L, false, 10L))
                    .thenReturn(List.of());

            service.execute(10L);

            verifyNoMoreInteractions(alertReceiptRepository);
        }
    }
}
