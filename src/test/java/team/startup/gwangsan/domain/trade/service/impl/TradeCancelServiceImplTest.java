package team.startup.gwangsan.domain.trade.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.AlreadyTradeCancelRequestException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeParticipantOnlyException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeCancelServiceImpl 단위 테스트")
class TradeCancelServiceImplTest {

    @InjectMocks private TradeCancelServiceImpl service;

    @Mock private MemberUtil memberUtil;
    @Mock private TradeCompleteRepository tradeCompleteRepository;
    @Mock private TradeCancelRepository tradeCancelRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private TradeCancelImageRepository tradeCancelImageRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("취소 요청을 저장하고 이벤트를 발행한다")
            void it_saves_trade_cancel_and_publishes_event() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(memberUtil.getCurrentMember()).thenReturn(member);

                Member buyer = mock(Member.class);
                when(buyer.getId()).thenReturn(1L);
                Member seller = mock(Member.class);
                when(seller.getId()).thenReturn(2L);

                TradeComplete tradeComplete = mock(TradeComplete.class);
                when(tradeComplete.getId()).thenReturn(10L);
                when(tradeComplete.getBuyer()).thenReturn(buyer);
                when(tradeComplete.getSeller()).thenReturn(seller);

                when(tradeCompleteRepository.findByProductIdAndStatus(100L, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(tradeComplete));
                when(tradeCancelRepository.existsByTradeCompleteIdAndStatus(10L, TradeCancelStatus.PENDING))
                        .thenReturn(false);

                TradeCancel savedCancel = mock(TradeCancel.class);
                when(savedCancel.getId()).thenReturn(20L);
                when(tradeCancelRepository.save(any())).thenReturn(savedCancel);

                Image image = mock(Image.class);
                when(imageRepository.findAllById(List.of(5L))).thenReturn(List.of(image));

                service.execute(100L, "이유", List.of(5L));

                verify(tradeCancelRepository).save(any());
                verify(tradeCancelImageRepository).saveAll(any());
                verify(applicationEventPublisher).publishEvent(any(CreateAdminAlertEvent.class));
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

                assertThatThrownBy(() -> service.execute(99L, "이유", List.of()))
                        .isInstanceOf(NotFoundTradeCompleteException.class);
            }
        }

        @Nested
        @DisplayName("거래에 참여하지 않은 제3자가 취소 요청 시")
        class Context_with_non_participant {

            @Test
            @DisplayName("TradeParticipantOnlyException을 던진다")
            void it_throws_trade_participant_only_exception() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(99L);
                when(memberUtil.getCurrentMember()).thenReturn(member);

                Member buyer = mock(Member.class);
                when(buyer.getId()).thenReturn(1L);
                Member seller = mock(Member.class);
                when(seller.getId()).thenReturn(2L);

                TradeComplete tradeComplete = mock(TradeComplete.class);
                when(tradeComplete.getBuyer()).thenReturn(buyer);
                when(tradeComplete.getSeller()).thenReturn(seller);

                when(tradeCompleteRepository.findByProductIdAndStatus(100L, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(tradeComplete));

                assertThatThrownBy(() -> service.execute(100L, "이유", List.of()))
                        .isInstanceOf(TradeParticipantOnlyException.class);
            }
        }

        @Nested
        @DisplayName("이미 취소 요청이 있을 때")
        class Context_with_already_requested {

            @Test
            @DisplayName("AlreadyTradeCancelRequestException을 던진다")
            void it_throws_already_trade_cancel_request_exception() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(memberUtil.getCurrentMember()).thenReturn(member);

                Member buyer = mock(Member.class);
                when(buyer.getId()).thenReturn(1L);
                Member seller = mock(Member.class);
                when(seller.getId()).thenReturn(2L);

                TradeComplete tradeComplete = mock(TradeComplete.class);
                when(tradeComplete.getId()).thenReturn(10L);
                when(tradeComplete.getBuyer()).thenReturn(buyer);
                when(tradeComplete.getSeller()).thenReturn(seller);

                when(tradeCompleteRepository.findByProductIdAndStatus(100L, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(tradeComplete));
                when(tradeCancelRepository.existsByTradeCompleteIdAndStatus(10L, TradeCancelStatus.PENDING))
                        .thenReturn(true);

                assertThatThrownBy(() -> service.execute(100L, "이유", List.of()))
                        .isInstanceOf(AlreadyTradeCancelRequestException.class);
            }
        }
    }
}
