package team.startup.gwangsan.application.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.data.WithdrawalTarget;
import team.startup.gwangsan.application.member.port.out.MemberReferenceCleanupPort;
import team.startup.gwangsan.application.member.port.out.MemberWithdrawalPort;
import team.startup.gwangsan.domain.member.exception.DummyMemberDeletionNotAllowedException;
import team.startup.gwangsan.domain.member.exception.NotFoundDummyMemberException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("WithdrawMemberService 단위 테스트")
class WithdrawMemberServiceTest {
    private final MemberWithdrawalPort members = mock(MemberWithdrawalPort.class);
    private final MemberReferenceCleanupPort references = mock(MemberReferenceCleanupPort.class);
    private final WithdrawMemberService service = new WithdrawMemberService(members, references);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("it_실제 광산으로 탈퇴 기록을 남기고 기존 순서대로 참조를 정리한 뒤 회원을 지운다")
    void it_records_and_cleans_up_in_order(boolean banned) {
        when(members.loadTarget(1L)).thenReturn(new WithdrawalTarget("010", 7300));
        when(members.findDummyMemberId()).thenReturn(Optional.of(99L));

        service.execute(new WithdrawMemberCommand(1L, banned));

        InOrder order = inOrder(members, references);
        order.verify(members).saveWithdrawalRecord("010", 7300, banned);
        order.verify(members).reassignRecommender(1L, 99L);
        order.verify(references).reassignReferences(1L, 99L);
        order.verify(references).deletePersonalData(1L);
        order.verify(members).deleteDetail(1L);
        order.verify(members).deleteMember(1L);
    }

    @Test
    @DisplayName("it_회원 상세가 없으면 아무것도 기록하지 않는다")
    void it_throws_when_detail_missing() {
        when(members.loadTarget(1L)).thenThrow(new NotFoundMemberDetailException());

        assertThatThrownBy(() -> service.execute(new WithdrawMemberCommand(1L, false)))
                .isInstanceOf(NotFoundMemberDetailException.class);
        verify(members, never()).saveWithdrawalRecord(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("it_dummy 회원이 없으면 NotFoundDummyMemberException 이고 참조를 건드리지 않는다")
    void it_throws_when_dummy_missing() {
        when(members.loadTarget(1L)).thenReturn(new WithdrawalTarget("010", 0));
        when(members.findDummyMemberId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new WithdrawMemberCommand(1L, false)))
                .isInstanceOf(NotFoundDummyMemberException.class);
        verifyNoInteractions(references);
    }

    @Test
    @DisplayName("it_dummy 회원 자신은 탈퇴시킬 수 없다")
    void it_rejects_dummy_itself() {
        when(members.loadTarget(99L)).thenReturn(new WithdrawalTarget("dummy", 0));
        when(members.findDummyMemberId()).thenReturn(Optional.of(99L));

        assertThatThrownBy(() -> service.execute(new WithdrawMemberCommand(99L, true)))
                .isInstanceOf(DummyMemberDeletionNotAllowedException.class);
        verifyNoInteractions(references);
        verify(members, never()).deleteMember(anyLong());
    }
}
