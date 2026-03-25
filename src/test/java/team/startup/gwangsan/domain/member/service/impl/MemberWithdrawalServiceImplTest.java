package team.startup.gwangsan.domain.member.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.repository.WithdrawalRecordRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberWithdrawalServiceImpl 단위 테스트")
class MemberWithdrawalServiceImplTest {

    @InjectMocks
    private MemberWithdrawalServiceImpl service;

    @Mock private MemberUtil memberUtil;
    @Mock private MemberRepository memberRepository;
    @Mock private WithdrawalRecordRepository withdrawalRecordRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상 탈퇴 시")
        class Context_with_valid_request {

            @Test
            @DisplayName("탈퇴 기록을 저장하고 회원을 삭제한다")
            void it_saves_withdrawal_record_and_deletes_member() {
                Member member = mock(Member.class);
                when(member.getPhoneNumber()).thenReturn("01012345678");
                when(memberUtil.getCurrentMember()).thenReturn(member);

                service.execute();

                ArgumentCaptor<WithdrawalRecord> captor = ArgumentCaptor.forClass(WithdrawalRecord.class);
                verify(withdrawalRecordRepository).save(captor.capture());
                assertThat(captor.getValue().getPhoneNumber()).isEqualTo("01012345678");
                assertThat(captor.getValue().getGwangsan()).isEqualTo(0);

                verify(memberRepository).delete(member);
            }
        }
    }
}
