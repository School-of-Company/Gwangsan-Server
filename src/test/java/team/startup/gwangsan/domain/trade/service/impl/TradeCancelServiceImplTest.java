package team.startup.gwangsan.domain.trade.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.AlreadyTradeCancelRequestException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeParticipantOnlyException;
import team.startup.gwangsan.domain.trade.presentation.dto.response.TradeCancelResponse;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.service.TradeCancelApplier;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeCancelServiceImpl 단위 테스트")
class TradeCancelServiceImplTest {

    private static final Long PRODUCT_ID = 100L;
    private static final Long TRADE_COMPLETE_ID = 10L;
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;

    @InjectMocks private TradeCancelServiceImpl service;

    @Mock private MemberUtil memberUtil;
    @Mock private ProductRepository productRepository;
    @Mock private TradeCompleteRepository tradeCompleteRepository;
    @Mock private TradeCancelRepository tradeCancelRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private TradeCancelImageRepository tradeCancelImageRepository;
    @Mock private TradeCancelApplier tradeCancelApplier;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    // 케이스마다 실제로 호출되는 getter 가 달라(구매자가 먼저 매치되면 판매자 getId() 는
    // 호출되지 않는다) 헬퍼가 만드는 목은 lenient 로 둔다. 동작 검증은 verify 로 한다.
    private Member member(Long id) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(id);
        return member;
    }

    /** 잠긴 상품 + COMPLETED 거래를 준비하고 그 거래를 돌려준다. */
    private TradeComplete givenLockedTrade() {
        Member buyer = member(BUYER_ID);
        Member seller = member(SELLER_ID);

        TradeComplete tradeComplete = mock(TradeComplete.class);
        lenient().when(tradeComplete.getId()).thenReturn(TRADE_COMPLETE_ID);
        lenient().when(tradeComplete.getBuyer()).thenReturn(buyer);
        lenient().when(tradeComplete.getSeller()).thenReturn(seller);

        Product product = mock(Product.class);
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(tradeCompleteRepository.findByProductIdAndStatus(PRODUCT_ID, TradeStatus.COMPLETED))
                .thenReturn(Optional.of(tradeComplete));

        return tradeComplete;
    }

    private TradeCancel pendingRequestedBy(Long memberId) {
        Member requester = member(memberId);
        TradeCancel tradeCancel = mock(TradeCancel.class);
        lenient().when(tradeCancel.getMember()).thenReturn(requester);
        return tradeCancel;
    }

    private void givenPending(TradeCancel tradeCancel) {
        when(tradeCancelRepository.findByTradeCompleteIdAndStatus(TRADE_COMPLETE_ID, TradeCancelStatus.PENDING))
                .thenReturn(Optional.ofNullable(tradeCancel));
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("대기 중인 철회 요청이 없을 때")
        class Context_without_pending_request {

            @Test
            @DisplayName("취소 요청을 저장하고 관리자 알림 이벤트를 발행한다")
            void it_saves_trade_cancel_and_publishes_event() {
                Member currentMember = member(BUYER_ID);
                when(memberUtil.getCurrentMember()).thenReturn(currentMember);
                givenLockedTrade();
                givenPending(null);

                TradeCancel saved = mock(TradeCancel.class);
                lenient().when(saved.getId()).thenReturn(20L);
                when(tradeCancelRepository.save(any())).thenReturn(saved);
                when(imageRepository.findAllById(List.of(5L))).thenReturn(List.of(mock(Image.class)));

                TradeCancelResponse response = service.execute(PRODUCT_ID, "이유", List.of(5L));

                // 관리자 승인 대기 상태임을 클라이언트가 알 수 있어야 한다.
                assertThat(response.cancelled()).isFalse();
                verify(tradeCancelRepository).save(any());
                verify(tradeCancelImageRepository).saveAll(any());
                verify(applicationEventPublisher).publishEvent(any(CreateAdminAlertEvent.class));
                verify(tradeCancelApplier, never()).apply(any());
            }

            @Test
            @DisplayName("상품 행을 비관적 잠금으로 조회한다")
            void it_locks_product_row() {
                Member currentMember = member(BUYER_ID);
                when(memberUtil.getCurrentMember()).thenReturn(currentMember);
                givenLockedTrade();
                givenPending(null);
                when(tradeCancelRepository.save(any())).thenReturn(mock(TradeCancel.class));

                service.execute(PRODUCT_ID, "이유", List.of());

                verify(productRepository).findByIdForUpdate(PRODUCT_ID);
            }
        }

        @Nested
        @DisplayName("상대방이 이미 철회를 요청했을 때")
        class Context_with_counterpart_request {

            @Test
            @DisplayName("양측 동의로 보고 즉시 철회하며 새 요청을 저장하지 않는다")
            void it_applies_cancel_immediately() {
                Member currentMember = member(SELLER_ID);
                when(memberUtil.getCurrentMember()).thenReturn(currentMember);
                givenLockedTrade();

                TradeCancel requestedByBuyer = pendingRequestedBy(BUYER_ID);
                givenPending(requestedByBuyer);

                TradeCancelResponse response = service.execute(PRODUCT_ID, "저도 철회합니다", List.of(5L));

                // 즉시 철회까지 끝났음을 알려야 한다. 응답 코드는 첫 요청과 같은 200 이다.
                assertThat(response.cancelled()).isTrue();
                verify(tradeCancelApplier).apply(requestedByBuyer);
                verify(tradeCancelRepository, never()).save(any());
                verify(tradeCancelImageRepository, never()).saveAll(any());
                verify(imageRepository, never()).findAllById(any());
                verify(applicationEventPublisher, never()).publishEvent(any(CreateAdminAlertEvent.class));
            }

            @Test
            @DisplayName("구매자가 동의하는 방향에서도 똑같이 즉시 철회한다")
            void it_applies_cancel_for_buyer_side_too() {
                Member currentMember = member(BUYER_ID);
                when(memberUtil.getCurrentMember()).thenReturn(currentMember);
                givenLockedTrade();

                TradeCancel requestedBySeller = pendingRequestedBy(SELLER_ID);
                givenPending(requestedBySeller);

                service.execute(PRODUCT_ID, "이유", List.of());

                verify(tradeCancelApplier).apply(requestedBySeller);
                verify(tradeCancelRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("같은 요청자가 다시 요청할 때")
        class Context_with_same_requester {

            @Test
            @DisplayName("AlreadyTradeCancelRequestException을 던진다")
            void it_throws_already_trade_cancel_request_exception() {
                Member currentMember = member(BUYER_ID);
                when(memberUtil.getCurrentMember()).thenReturn(currentMember);
                givenLockedTrade();
                givenPending(pendingRequestedBy(BUYER_ID));

                assertThatThrownBy(() -> service.execute(PRODUCT_ID, "이유", List.of()))
                        .isInstanceOf(AlreadyTradeCancelRequestException.class);

                verify(tradeCancelApplier, never()).apply(any());
                verify(tradeCancelRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("판매자가 게시글을 삭제한 뒤에도")
        class Context_with_deleted_product {

            @Test
            @DisplayName("삭제 여부를 가리지 않는 잠금을 써서 철회를 막지 않는다")
            void it_does_not_block_cancel_for_deleted_product() {
                Member currentMember = member(SELLER_ID);
                when(memberUtil.getCurrentMember()).thenReturn(currentMember);
                givenLockedTrade();

                TradeCancel requestedByBuyer = pendingRequestedBy(BUYER_ID);
                givenPending(requestedByBuyer);

                service.execute(PRODUCT_ID, "이유", List.of());

                // findByIdWithLock 은 DELETED 를 제외하므로 여기서 쓰면 환불이 영구히 막힌다.
                verify(productRepository).findByIdForUpdate(PRODUCT_ID);
                verify(productRepository, never()).findByIdWithLock(any());
                verify(tradeCancelApplier).apply(requestedByBuyer);
            }
        }

        @Nested
        @DisplayName("상품이 없을 때")
        class Context_with_product_not_found {

            @Test
            @DisplayName("NotFoundProductException을 던진다")
            void it_throws_not_found_product_exception() {
                when(memberUtil.getCurrentMember()).thenReturn(mock(Member.class));
                when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(PRODUCT_ID, "이유", List.of()))
                        .isInstanceOf(NotFoundProductException.class);
            }
        }

        @Nested
        @DisplayName("거래 완료 내역이 없을 때")
        class Context_with_trade_complete_not_found {

            @Test
            @DisplayName("NotFoundTradeCompleteException을 던진다")
            void it_throws_not_found_trade_complete_exception() {
                when(memberUtil.getCurrentMember()).thenReturn(mock(Member.class));
                when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(mock(Product.class)));
                when(tradeCompleteRepository.findByProductIdAndStatus(PRODUCT_ID, TradeStatus.COMPLETED))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(PRODUCT_ID, "이유", List.of()))
                        .isInstanceOf(NotFoundTradeCompleteException.class);
            }
        }

        @Nested
        @DisplayName("거래에 참여하지 않은 제3자가 취소 요청 시")
        class Context_with_non_participant {

            @Test
            @DisplayName("TradeParticipantOnlyException을 던진다")
            void it_throws_trade_participant_only_exception() {
                Member currentMember = member(99L);
                when(memberUtil.getCurrentMember()).thenReturn(currentMember);
                givenLockedTrade();

                assertThatThrownBy(() -> service.execute(PRODUCT_ID, "이유", List.of()))
                        .isInstanceOf(TradeParticipantOnlyException.class);

                verify(tradeCancelApplier, never()).apply(any());
            }
        }
    }
}
