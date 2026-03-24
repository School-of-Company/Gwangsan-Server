package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApproveTradeCancelServiceImpl 단위 테스트")
class ApproveTradeCancelServiceImplTest {

    @InjectMocks
    private ApproveTradeCancelServiceImpl service;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private TradeCancelRepository tradeCancelRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("PENDING 상태의 거래 취소 승인 시")
        class Context_with_pending_trade_cancel {

            @Test
            @DisplayName("광산을 환불하고 상태를 APPROVED/ROLLED_BACK으로 변경 후 이벤트를 발행한다")
            void it_approves_trade_cancel() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(4L);
                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getSourceId()).thenReturn(5L);
                when(alert.getRequester()).thenReturn(requester);

                MemberDetail adminDetail = mock(MemberDetail.class);

                Member buyer = mock(Member.class);
                when(buyer.getId()).thenReturn(2L);
                Member seller = mock(Member.class);
                when(seller.getId()).thenReturn(3L);

                MemberDetail buyerDetail = mock(MemberDetail.class);
                MemberDetail sellerDetail = mock(MemberDetail.class);

                Product product = mock(Product.class);
                when(product.getGwangsan()).thenReturn(1000);

                TradeComplete tradeComplete = mock(TradeComplete.class);
                when(tradeComplete.getProduct()).thenReturn(product);
                when(tradeComplete.getBuyer()).thenReturn(buyer);
                when(tradeComplete.getSeller()).thenReturn(seller);

                TradeCancel tradeCancel = mock(TradeCancel.class);
                when(tradeCancel.getStatus()).thenReturn(TradeCancelStatus.PENDING);
                when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);
                when(tradeCancel.getId()).thenReturn(5L);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(adminAlertRepository.findByIdWithMember(10L)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(tradeCancelRepository.findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(5L))
                        .thenReturn(Optional.of(tradeCancel));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(buyerDetail));
                when(memberDetailRepository.findById(3L)).thenReturn(Optional.of(sellerDetail));

                service.execute(10L);

                verify(buyerDetail).plusGwangsan(1000);
                verify(sellerDetail).minusGwangsan(1000);
                verify(tradeCancel).updateStatus(TradeCancelStatus.APPROVED);
                verify(tradeComplete).updateStatus(TradeStatus.ROLLED_BACK);
                verify(applicationEventPublisher).publishEvent(any(CreateAlertEvent.class));
            }
        }

        @Nested
        @DisplayName("PENDING 상태가 아닌 거래 취소 승인 시")
        class Context_with_non_pending_trade_cancel {

            @Test
            @DisplayName("CannotPendingTradeCancelException을 던진다")
            void it_throws_cannot_pending_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);

                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getSourceId()).thenReturn(5L);

                MemberDetail adminDetail = mock(MemberDetail.class);

                TradeCancel tradeCancel = mock(TradeCancel.class);
                when(tradeCancel.getStatus()).thenReturn(TradeCancelStatus.APPROVED);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(adminAlertRepository.findByIdWithMember(10L)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(tradeCancelRepository.findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(5L))
                        .thenReturn(Optional.of(tradeCancel));

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(CannotPendingTradeCancelException.class);
            }
        }

        @Nested
        @DisplayName("AlertId에 해당하는 알림이 없을 때")
        class Context_with_alert_not_found {

            @Test
            @DisplayName("NotFoundAdminAlertException을 던진다")
            void it_throws_not_found_admin_alert_exception() {
                Member admin = mock(Member.class);
                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(adminAlertRepository.findByIdWithMember(10L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(NotFoundAdminAlertException.class);
            }
        }

        @Nested
        @DisplayName("TradeCancel이 없을 때")
        class Context_with_trade_cancel_not_found {

            @Test
            @DisplayName("NotFoundTradeCancelException을 던진다")
            void it_throws_not_found_trade_cancel_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);

                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getSourceId()).thenReturn(5L);

                MemberDetail adminDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(adminAlertRepository.findByIdWithMember(10L)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(tradeCancelRepository.findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(5L))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(NotFoundTradeCancelException.class);
            }
        }
    }
}
