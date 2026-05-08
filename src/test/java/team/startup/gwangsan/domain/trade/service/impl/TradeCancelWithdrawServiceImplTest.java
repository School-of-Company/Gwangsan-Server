package team.startup.gwangsan.domain.trade.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.NotTradeCancelRequesterException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeCancelWithdrawServiceImpl 단위 테스트")
class TradeCancelWithdrawServiceImplTest {

    @InjectMocks private TradeCancelWithdrawServiceImpl service;

    @Mock private MemberUtil memberUtil;
    @Mock private TradeCompleteRepository tradeCompleteRepository;
    @Mock private TradeCancelRepository tradeCancelRepository;
    @Mock private TradeCancelImageRepository tradeCancelImageRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("취소 요청자 본인이 철회 시도 시")
        class Context_with_valid_request {

            @Test
            @DisplayName("취소 상태를 WITHDRAWN으로 변경하고 이미지를 삭제한다")
            void it_withdraws_trade_cancel() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(memberUtil.getCurrentMember()).thenReturn(member);

                TradeComplete tradeComplete = mock(TradeComplete.class);
                when(tradeComplete.getId()).thenReturn(10L);
                when(tradeCompleteRepository.findByProductIdAndStatus(100L, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(tradeComplete));

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(1L);

                TradeCancel tradeCancel = mock(TradeCancel.class);
                when(tradeCancel.getId()).thenReturn(20L);
                when(tradeCancel.getMember()).thenReturn(requester);
                when(tradeCancelRepository.findByTradeCompleteIdAndStatus(10L, TradeCancelStatus.PENDING))
                        .thenReturn(Optional.of(tradeCancel));

                service.execute(100L);

                verify(tradeCancel).updateStatus(TradeCancelStatus.WITHDRAWN);
                verify(tradeCancelImageRepository).deleteByTradeCancelId(20L);
            }
        }

        @Nested
        @DisplayName("거래 완료 내역이 없을 때")
        class Context_with_trade_complete_not_found {

            @Test
            @DisplayName("NotFoundTradeCompleteException을 던진다")
            void it_throws_not_found_trade_complete_exception() {
                Member member = mock(Member.class);
                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(tradeCompleteRepository.findByProductIdAndStatus(99L, TradeStatus.COMPLETED))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(99L))
                        .isInstanceOf(NotFoundTradeCompleteException.class);
            }
        }

        @Nested
        @DisplayName("취소 요청 내역이 없을 때")
        class Context_with_trade_cancel_not_found {

            @Test
            @DisplayName("NotFoundTradeCancelException을 던진다")
            void it_throws_not_found_trade_cancel_exception() {
                Member member = mock(Member.class);
                when(memberUtil.getCurrentMember()).thenReturn(member);

                TradeComplete tradeComplete = mock(TradeComplete.class);
                when(tradeComplete.getId()).thenReturn(10L);
                when(tradeCompleteRepository.findByProductIdAndStatus(100L, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(tradeComplete));
                when(tradeCancelRepository.findByTradeCompleteIdAndStatus(10L, TradeCancelStatus.PENDING))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(100L))
                        .isInstanceOf(NotFoundTradeCancelException.class);
            }
        }

        @Nested
        @DisplayName("취소 요청자가 아닌 제3자가 철회 시도 시")
        class Context_with_non_requester_withdraws {

            @Test
            @DisplayName("NotTradeCancelRequesterException을 던진다")
            void it_throws_not_trade_cancel_requester_exception() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(memberUtil.getCurrentMember()).thenReturn(member);

                TradeComplete tradeComplete = mock(TradeComplete.class);
                when(tradeComplete.getId()).thenReturn(10L);
                when(tradeCompleteRepository.findByProductIdAndStatus(100L, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(tradeComplete));

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(2L);

                TradeCancel tradeCancel = mock(TradeCancel.class);
                when(tradeCancel.getMember()).thenReturn(requester);
                when(tradeCancelRepository.findByTradeCompleteIdAndStatus(10L, TradeCancelStatus.PENDING))
                        .thenReturn(Optional.of(tradeCancel));

                assertThatThrownBy(() -> service.execute(100L))
                        .isInstanceOf(NotTradeCancelRequesterException.class);
            }
        }
    }
}
