package team.startup.gwangsan.domain.trade.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.presentation.dto.response.GetTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.constant.TradeRole;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindMyTradeHistoryServiceImpl 단위 테스트")
class FindMyTradeHistoryServiceImplTest {

    private static final Long ME_ID = 100L;
    private static final Long OPPONENT_ID = 200L;

    @Mock
    private TradeCompleteRepository tradeCompleteRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private MemberUtil memberUtil;

    @InjectMocks
    private FindMyTradeHistoryServiceImpl service;

    private Member memberOf(Long id, String nickname) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(id);
        lenient().when(member.getNickname()).thenReturn(nickname);
        return member;
    }

    private Product productOf(Long id, String title, Integer gwangsan) {
        Product product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(id);
        lenient().when(product.getTitle()).thenReturn(title);
        lenient().when(product.getGwangsan()).thenReturn(gwangsan);
        return product;
    }

    private TradeComplete tradeOf(Long id, Product product, Member buyer, Member seller, LocalDateTime completedAt) {
        TradeComplete trade = mock(TradeComplete.class);
        lenient().when(trade.getId()).thenReturn(id);
        lenient().when(trade.getProduct()).thenReturn(product);
        lenient().when(trade.getBuyer()).thenReturn(buyer);
        lenient().when(trade.getSeller()).thenReturn(seller);
        lenient().when(trade.getStatus()).thenReturn(TradeStatus.COMPLETED);
        lenient().when(trade.getCompletedAt()).thenReturn(completedAt);
        return trade;
    }

    private ProductImage productImageOf(Product product, Long imageId, String url) {
        Image image = mock(Image.class);
        lenient().when(image.getId()).thenReturn(imageId);
        lenient().when(image.getImageUrl()).thenReturn(url);

        ProductImage productImage = mock(ProductImage.class);
        lenient().when(productImage.getProduct()).thenReturn(product);
        lenient().when(productImage.getImage()).thenReturn(image);
        return productImage;
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("내가 buyer 인 거래는 role 을 BUYER 로, seller 인 거래는 SELLER 로 내려준다")
        void it_maps_role_by_whether_i_am_buyer_or_seller() {
            Member me = memberOf(ME_ID, "나");
            Member opponent = memberOf(OPPONENT_ID, "상대방");

            Product bought = productOf(1L, "내가 산 물건", 5000);
            Product sold = productOf(2L, "내가 판 물건", 10000);

            TradeComplete boughtTrade = tradeOf(11L, bought, me, opponent, LocalDateTime.of(2026, 8, 20, 10, 0));
            TradeComplete soldTrade = tradeOf(12L, sold, opponent, me, LocalDateTime.of(2026, 8, 19, 10, 0));

            when(memberUtil.getCurrentMember()).thenReturn(me);
            when(tradeCompleteRepository.findAllByMemberAndStatus(me, TradeStatus.COMPLETED))
                    .thenReturn(List.of(boughtTrade, soldTrade));
            when(productImageRepository.findAllByProductIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of());

            List<GetTradeHistoryResponse> result = service.execute(TradeStatus.COMPLETED);

            assertThat(result).hasSize(2);

            GetTradeHistoryResponse first = result.get(0);
            assertThat(first.tradeId()).isEqualTo(11L);
            assertThat(first.role()).isEqualTo(TradeRole.BUYER);
            assertThat(first.status()).isEqualTo(TradeStatus.COMPLETED);
            assertThat(first.completedAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
            assertThat(first.otherMember().memberId()).isEqualTo(OPPONENT_ID);
            assertThat(first.otherMember().nickname()).isEqualTo("상대방");
            assertThat(first.product().productId()).isEqualTo(1L);
            assertThat(first.product().title()).isEqualTo("내가 산 물건");
            assertThat(first.product().gwangsan()).isEqualTo(5000);

            assertThat(result.get(1).role()).isEqualTo(TradeRole.SELLER);
            assertThat(result.get(1).otherMember().memberId()).isEqualTo(OPPONENT_ID);
            assertThat(result.get(1).otherMember().nickname()).isEqualTo("상대방");
            assertThat(result.get(1).product().title()).isEqualTo("내가 판 물건");
        }

        @Test
        @DisplayName("상품 이미지가 여러 장이면 먼저 조회된 것을 대표 이미지로 내려준다")
        void it_picks_first_image_as_thumbnail() {
            Member me = memberOf(ME_ID, "나");
            Member opponent = memberOf(OPPONENT_ID, "상대방");
            Product product = productOf(1L, "사진 여러 장", 5000);
            TradeComplete trade = tradeOf(11L, product, me, opponent, LocalDateTime.of(2026, 8, 20, 10, 0));

            ProductImage firstImage = productImageOf(product, 31L, "https://example.com/first.png");
            ProductImage secondImage = productImageOf(product, 32L, "https://example.com/second.png");

            when(memberUtil.getCurrentMember()).thenReturn(me);
            when(tradeCompleteRepository.findAllByMemberAndStatus(me, TradeStatus.COMPLETED))
                    .thenReturn(List.of(trade));
            when(productImageRepository.findAllByProductIdIn(List.of(1L)))
                    .thenReturn(List.of(firstImage, secondImage));

            List<GetTradeHistoryResponse> result = service.execute(TradeStatus.COMPLETED);

            assertThat(result.get(0).product().image().imageId()).isEqualTo(31L);
            assertThat(result.get(0).product().image().imageUrl()).isEqualTo("https://example.com/first.png");
        }

        @Test
        @DisplayName("이미지가 없는 상품은 대표 이미지를 null 로 내려준다")
        void it_returns_null_image_when_product_has_no_image() {
            Member me = memberOf(ME_ID, "나");
            Member opponent = memberOf(OPPONENT_ID, "상대방");
            Product product = productOf(1L, "사진 없음", 5000);
            TradeComplete trade = tradeOf(11L, product, me, opponent, null);

            when(memberUtil.getCurrentMember()).thenReturn(me);
            when(tradeCompleteRepository.findAllByMemberAndStatus(me, TradeStatus.COMPLETED))
                    .thenReturn(List.of(trade));
            when(productImageRepository.findAllByProductIdIn(List.of(1L))).thenReturn(List.of());

            List<GetTradeHistoryResponse> result = service.execute(TradeStatus.COMPLETED);

            assertThat(result.get(0).product().image()).isNull();
            assertThat(result.get(0).completedAt()).isNull();
        }

        @Test
        @DisplayName("거래가 없으면 빈 목록을 반환하고 이미지를 조회하지 않는다")
        void it_returns_empty_list_without_querying_images() {
            Member me = memberOf(ME_ID, "나");

            when(memberUtil.getCurrentMember()).thenReturn(me);
            when(tradeCompleteRepository.findAllByMemberAndStatus(me, TradeStatus.COMPLETED))
                    .thenReturn(List.of());

            List<GetTradeHistoryResponse> result = service.execute(TradeStatus.COMPLETED);

            assertThat(result).isEmpty();
            verify(productImageRepository, never()).findAllByProductIdIn(any());
        }

        @Test
        @DisplayName("요청한 status 를 그대로 조회 조건에 넘긴다")
        void it_passes_requested_status_to_repository() {
            Member me = memberOf(ME_ID, "나");

            when(memberUtil.getCurrentMember()).thenReturn(me);
            when(tradeCompleteRepository.findAllByMemberAndStatus(me, TradeStatus.PENDING))
                    .thenReturn(List.of());

            service.execute(TradeStatus.PENDING);

            verify(tradeCompleteRepository).findAllByMemberAndStatus(me, TradeStatus.PENDING);
        }
    }
}
