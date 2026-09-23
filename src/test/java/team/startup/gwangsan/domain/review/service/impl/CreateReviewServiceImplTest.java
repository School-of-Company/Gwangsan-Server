package team.startup.gwangsan.domain.review.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.review.exception.AlreadyReviewedException;
import team.startup.gwangsan.domain.review.exception.CannotReviewBeforeTradeException;
import team.startup.gwangsan.domain.review.exception.CannotReviewSelfException;
import team.startup.gwangsan.domain.review.exception.NotTradeParticipantException;
import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;
import team.startup.gwangsan.global.event.CreateAlertEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateReviewServiceImpl 단위 테스트")
class CreateReviewServiceImplTest {

    @InjectMocks private CreateReviewServiceImpl service;

    @Mock private ReviewRepository reviewRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private MemberRepository memberRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private TradeCompleteRepository tradeCompleteRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    @Mock private BlockValidator blockValidator;

    private Member mockMember(Long id) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(id);
        return member;
    }

    private TradeComplete mockCompletedTrade(Member buyer, Member seller) {
        TradeComplete tradeComplete = mock(TradeComplete.class);
        lenient().when(tradeComplete.getBuyer()).thenReturn(buyer);
        lenient().when(tradeComplete.getSeller()).thenReturn(seller);
        return tradeComplete;
    }

    private Member member(Long id) {
        Member member = Member.builder()
                .name("회원" + id)
                .nickname("회원" + id)
                .phoneNumber("0100000000" + id)
                .password("password")
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Product completedProduct(Member seller) {
        return Product.builder()
                .title("거래")
                .description("완료된 거래")
                .gwangsan(5000)
                .member(seller)
                .status(ProductStatus.COMPLETED)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .build();
    }

    private MemberDetail detail(Member member) {
        return MemberDetail.builder()
                .member(member)
                .dong(mock(team.startup.gwangsan.domain.dong.entity.Dong.class))
                .place(mock(team.startup.gwangsan.domain.place.entity.Place.class))
                .gwangsan(0)
                .light(1)
                .description("소개")
                .build();
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("리뷰를 저장하고 이벤트를 발행한다")
            void it_saves_review_and_publishes_event() {
                Member reviewer = mockMember(1L);
                Member reviewed = mockMember(2L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

                MemberDetail reviewedDetail = mock(MemberDetail.class);
                TradeComplete completedTrade = mockCompletedTrade(reviewer, reviewed);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(reviewed));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(completedTrade));
                when(reviewRepository.existsByProductAndReviewer(product, reviewer)).thenReturn(false);
                when(memberDetailRepository.findByMember(reviewed)).thenReturn(Optional.of(reviewedDetail));

                CreateReviewRequest request = new CreateReviewRequest(1L, 2L, "좋았어요", 80);
                service.execute(request);

                verify(reviewedDetail).plusLight(80);
                verify(reviewRepository).save(any());
                verify(applicationEventPublisher).publishEvent(any(Object.class));
            }
        }

        @Nested
        @DisplayName("판매자가 구매자를 리뷰할 때")
        class Context_with_seller_reviewing_buyer {

            @Test
            @DisplayName("구매자의 실제 점수만 올리고 리뷰와 알림을 만든다")
            void it_creates_review_for_buyer() {
                Member buyer = member(1L);
                Member seller = member(2L);
                Product product = completedProduct(seller);
                TradeComplete trade = TradeComplete.builder()
                        .product(product)
                        .buyer(buyer)
                        .seller(seller)
                        .status(TradeStatus.COMPLETED)
                        .requestedBySeller(true)
                        .build();
                MemberDetail buyerDetail = detail(buyer);

                when(memberUtil.getCurrentMember()).thenReturn(seller);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(trade));
                when(reviewRepository.existsByProductAndReviewer(product, seller)).thenReturn(false);
                when(memberDetailRepository.findByMember(buyer)).thenReturn(Optional.of(buyerDetail));

                service.execute(new CreateReviewRequest(1L, buyer.getId(), "좋았어요", 80));

                ArgumentCaptor<team.startup.gwangsan.domain.review.entity.Review> review =
                        ArgumentCaptor.forClass(team.startup.gwangsan.domain.review.entity.Review.class);
                ArgumentCaptor<CreateAlertEvent> event = ArgumentCaptor.forClass(CreateAlertEvent.class);
                verify(reviewRepository).save(review.capture());
                verify(applicationEventPublisher).publishEvent(event.capture());
                assertThat(review.getValue().getReviewer()).isSameAs(seller);
                assertThat(review.getValue().getReviewed()).isSameAs(buyer);
                assertThat(buyerDetail.getLight()).isEqualTo(81);
                assertThat(event.getValue().memberId()).isEqualTo(buyer.getId());
            }
        }

        @Nested
        @DisplayName("거래 상대가 아닌 회원을 리뷰 대상으로 지정했을 때")
        class Context_with_non_participant_review_target {

            @Test
            @DisplayName("구매자는 외부인을 리뷰할 수 없고 부작용이 없다")
            void it_rejects_buyer_reviewing_outsider_without_side_effects() {
                Member buyer = member(1L);
                Member seller = member(2L);
                Member outsider = member(3L);
                Product product = completedProduct(seller);
                TradeComplete trade = TradeComplete.builder()
                        .product(product).buyer(buyer).seller(seller)
                        .status(TradeStatus.COMPLETED).requestedBySeller(false).build();

                when(memberUtil.getCurrentMember()).thenReturn(buyer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(trade));

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, outsider.getId(), "내용", 50)))
                        .isInstanceOf(NotTradeParticipantException.class);

                verifyNoInteractions(reviewRepository, memberDetailRepository, applicationEventPublisher);
            }

            @Test
            @DisplayName("판매자는 외부인을 리뷰할 수 없고 부작용이 없다")
            void it_rejects_seller_reviewing_outsider_without_side_effects() {
                Member buyer = member(1L);
                Member seller = member(2L);
                Member outsider = member(3L);
                Product product = completedProduct(seller);
                TradeComplete trade = TradeComplete.builder()
                        .product(product).buyer(buyer).seller(seller)
                        .status(TradeStatus.COMPLETED).requestedBySeller(false).build();

                when(memberUtil.getCurrentMember()).thenReturn(seller);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(trade));

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, outsider.getId(), "내용", 50)))
                        .isInstanceOf(NotTradeParticipantException.class);

                verifyNoInteractions(reviewRepository, memberDetailRepository, applicationEventPublisher);
            }

            @Test
            @DisplayName("외부인은 구매자를 리뷰할 수 없고 부작용이 없다")
            void it_rejects_outsider_reviewing_buyer_without_side_effects() {
                Member buyer = member(1L);
                Member seller = member(2L);
                Member outsider = member(3L);
                Product product = completedProduct(seller);
                TradeComplete trade = TradeComplete.builder()
                        .product(product).buyer(buyer).seller(seller)
                        .status(TradeStatus.COMPLETED).requestedBySeller(false).build();

                when(memberUtil.getCurrentMember()).thenReturn(outsider);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(trade));

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, buyer.getId(), "내용", 50)))
                        .isInstanceOf(NotTradeParticipantException.class);

                verifyNoInteractions(reviewRepository, memberDetailRepository, applicationEventPublisher);
            }
        }

        @Nested
        @DisplayName("본인을 리뷰 대상으로 지정했을 때")
        class Context_with_self_review {

            @Test
            @DisplayName("CannotReviewSelfException을 던진다")
            void it_throws_cannot_review_self_exception() {
                Member reviewer = mockMember(1L);
                when(memberUtil.getCurrentMember()).thenReturn(reviewer);

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, 1L, "내용", 50)))
                        .isInstanceOf(CannotReviewSelfException.class);
            }
        }

        @Nested
        @DisplayName("상품이 없을 때")
        class Context_with_product_not_found {

            @Test
            @DisplayName("NotFoundProductException을 던진다")
            void it_throws_not_found_product_exception() {
                Member reviewer = mockMember(1L);
                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(99L, 2L, "내용", 50)))
                        .isInstanceOf(NotFoundProductException.class);
            }
        }

        @Nested
        @DisplayName("거래 완료 전 리뷰 시도 시")
        class Context_with_product_not_completed {

            @Test
            @DisplayName("CannotReviewBeforeTradeException을 던진다")
            void it_throws_cannot_review_before_trade_exception() {
                Member reviewer = mockMember(1L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.ONGOING);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, 2L, "내용", 50)))
                        .isInstanceOf(CannotReviewBeforeTradeException.class);
            }
        }

        @Nested
        @DisplayName("리뷰 대상 회원이 없을 때")
        class Context_with_target_member_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                Member reviewer = mockMember(1L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(2L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, 2L, "내용", 50)))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }

        @Nested
        @DisplayName("완료된 거래 내역을 찾을 수 없을 때")
        class Context_with_completed_trade_not_found {

            @Test
            @DisplayName("CannotReviewBeforeTradeException을 던진다")
            void it_throws_cannot_review_before_trade_exception() {
                Member reviewer = mockMember(1L);
                Member reviewed = mockMember(2L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(reviewed));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, 2L, "내용", 50)))
                        .isInstanceOf(CannotReviewBeforeTradeException.class);
            }
        }

        @Nested
        @DisplayName("지정한 대상이 거래 당사자가 아닐 때")
        class Context_with_not_trade_participant {

            @Test
            @DisplayName("NotTradeParticipantException을 던진다")
            void it_throws_not_trade_participant_exception() {
                Member reviewer = mockMember(1L);
                Member reviewed = mockMember(2L);
                Member thirdParty = mockMember(3L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

                TradeComplete completedTrade = mockCompletedTrade(reviewer, thirdParty);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(reviewed));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(completedTrade));

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, 2L, "내용", 50)))
                        .isInstanceOf(NotTradeParticipantException.class);
            }
        }

        @Nested
        @DisplayName("이미 리뷰를 작성한 경우")
        class Context_with_already_reviewed {

            @Test
            @DisplayName("AlreadyReviewedException을 던진다")
            void it_throws_already_reviewed_exception() {
                Member reviewer = mockMember(1L);
                Member reviewed = mockMember(2L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

                TradeComplete completedTrade = mockCompletedTrade(reviewer, reviewed);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(reviewed));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(completedTrade));
                when(reviewRepository.existsByProductAndReviewer(product, reviewer)).thenReturn(true);

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, 2L, "내용", 50)))
                        .isInstanceOf(AlreadyReviewedException.class);
            }
        }

        @Nested
        @DisplayName("피리뷰어 MemberDetail이 없을 때")
        class Context_with_reviewed_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberDetailException을 던진다")
            void it_throws_not_found_member_detail_exception() {
                Member reviewer = mockMember(1L);
                Member reviewed = mockMember(2L);

                Product product = mock(Product.class);
                when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

                TradeComplete completedTrade = mockCompletedTrade(reviewer, reviewed);

                when(memberUtil.getCurrentMember()).thenReturn(reviewer);
                when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(reviewed));
                when(tradeCompleteRepository.findByProductAndStatus(product, TradeStatus.COMPLETED))
                        .thenReturn(Optional.of(completedTrade));
                when(reviewRepository.existsByProductAndReviewer(product, reviewer)).thenReturn(false);
                when(memberDetailRepository.findByMember(reviewed)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(new CreateReviewRequest(1L, 2L, "내용", 50)))
                        .isInstanceOf(NotFoundMemberDetailException.class);
            }
        }
    }
}
