package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.service.TradeCancelApplier;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApproveTradeCancelServiceImpl 단위 테스트")
class ApproveTradeCancelServiceImplTest {

    private static final Long ALERT_ID = 10L;
    private static final Long TRADE_CANCEL_ID = 5L;
    private static final Long PRODUCT_ID = 7L;
    private static final Long ADMIN_ID = 1L;
    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 3L;

    @InjectMocks private ApproveTradeCancelServiceImpl service;

    @Mock private AdminAlertRepository adminAlertRepository;
    @Mock private TradeCancelRepository tradeCancelRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private ValidatePlaceUtil validatePlaceUtil;
    @Mock private TradeCancelApplier tradeCancelApplier;

    private Member member(Long id) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(id);
        return member;
    }

    /** 관리자 로그인 + 알림 조회까지 준비한다. */
    private void givenAdminAndAlert() {
        Member admin = member(ADMIN_ID);
        when(memberUtil.getCurrentMember()).thenReturn(admin);

        AdminAlert alert = mock(AdminAlert.class);
        lenient().when(alert.getSourceId()).thenReturn(TRADE_CANCEL_ID);
        when(adminAlertRepository.findByIdWithMember(ALERT_ID)).thenReturn(Optional.of(alert));

        when(memberDetailRepository.findById(ADMIN_ID)).thenReturn(Optional.of(mock(MemberDetail.class)));
    }

    /** 잠금까지 통과한 뒤 반환될 TradeCancel 을 준비한다. */
    private TradeCancel givenLockedTradeCancel() {
        when(tradeCancelRepository.findProductIdById(TRADE_CANCEL_ID)).thenReturn(Optional.of(PRODUCT_ID));
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(mock(Product.class)));

        Member buyer = member(BUYER_ID);
        Member seller = member(SELLER_ID);

        TradeComplete tradeComplete = mock(TradeComplete.class);
        lenient().when(tradeComplete.getBuyer()).thenReturn(buyer);
        lenient().when(tradeComplete.getSeller()).thenReturn(seller);

        TradeCancel tradeCancel = mock(TradeCancel.class);
        lenient().when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);

        when(tradeCancelRepository.findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(TRADE_CANCEL_ID))
                .thenReturn(Optional.of(tradeCancel));

        lenient().when(memberDetailRepository.findById(BUYER_ID)).thenReturn(Optional.of(mock(MemberDetail.class)));
        lenient().when(memberDetailRepository.findById(SELLER_ID)).thenReturn(Optional.of(mock(MemberDetail.class)));

        return tradeCancel;
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상 승인 시")
        class Context_with_valid_alert {

            @Test
            @DisplayName("상품 행을 잠근 뒤 철회 반영을 TradeCancelApplier 에 위임한다")
            void it_delegates_to_applier() {
                givenAdminAndAlert();
                TradeCancel tradeCancel = givenLockedTradeCancel();

                service.execute(ALERT_ID);

                verify(productRepository).findByIdForUpdate(PRODUCT_ID);
                verify(tradeCancelApplier).apply(tradeCancel);
            }

            @Test
            @DisplayName("TradeCancel 은 잠금을 잡은 뒤에 조회한다")
            void it_loads_trade_cancel_after_locking() {
                givenAdminAndAlert();
                givenLockedTradeCancel();

                service.execute(ALERT_ID);

                // 잠금 전에 엔티티를 읽으면 1차 캐시의 옛 상태를 보고 광산이 두 번 환불된다.
                InOrder order = inOrder(tradeCancelRepository, productRepository);
                order.verify(tradeCancelRepository).findProductIdById(TRADE_CANCEL_ID);
                order.verify(productRepository).findByIdForUpdate(PRODUCT_ID);
                order.verify(tradeCancelRepository).findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(TRADE_CANCEL_ID);
            }

            @Test
            @DisplayName("구매자와 판매자 모두 관리자와 같은 지역인지 검증한다")
            void it_validates_place_for_both_sides() {
                givenAdminAndAlert();
                givenLockedTradeCancel();

                service.execute(ALERT_ID);

                verify(validatePlaceUtil, times(2)).validateSamePlace(any(), any(), any());
            }

            @Test
            @DisplayName("지역이 다르면 철회를 반영하지 않는다")
            void it_does_not_apply_when_place_mismatched() {
                givenAdminAndAlert();
                givenLockedTradeCancel();

                doThrow(new PlaceMismatchException()).when(validatePlaceUtil).validateSamePlace(any(), any(), any());

                assertThatThrownBy(() -> service.execute(ALERT_ID))
                        .isInstanceOf(PlaceMismatchException.class);

                verify(tradeCancelApplier, never()).apply(any());
            }
        }

        @Nested
        @DisplayName("AlertId에 해당하는 알림이 없을 때")
        class Context_with_alert_not_found {

            @Test
            @DisplayName("NotFoundAdminAlertException을 던진다")
            void it_throws_not_found_admin_alert_exception() {
                when(memberUtil.getCurrentMember()).thenReturn(mock(Member.class));
                when(adminAlertRepository.findByIdWithMember(ALERT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(ALERT_ID))
                        .isInstanceOf(NotFoundAdminAlertException.class);
            }
        }

        @Nested
        @DisplayName("TradeCancel이 없을 때")
        class Context_with_trade_cancel_not_found {

            @Test
            @DisplayName("NotFoundTradeCancelException을 던진다")
            void it_throws_not_found_trade_cancel_exception() {
                givenAdminAndAlert();
                when(tradeCancelRepository.findProductIdById(TRADE_CANCEL_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(ALERT_ID))
                        .isInstanceOf(NotFoundTradeCancelException.class);

                verify(productRepository, never()).findByIdForUpdate(any());
            }
        }

        @Nested
        @DisplayName("상품이 삭제되었을 때")
        class Context_with_product_not_found {

            @Test
            @DisplayName("NotFoundProductException을 던진다")
            void it_throws_not_found_product_exception() {
                givenAdminAndAlert();
                when(tradeCancelRepository.findProductIdById(TRADE_CANCEL_ID)).thenReturn(Optional.of(PRODUCT_ID));
                when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(ALERT_ID))
                        .isInstanceOf(NotFoundProductException.class);

                verify(tradeCancelApplier, never()).apply(any());
            }
        }
    }
}
