package team.startup.gwangsan.application.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.post.data.AuthorProfile;
import team.startup.gwangsan.application.post.data.ChatRoomView;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.MemberProductsCriteria;
import team.startup.gwangsan.application.post.data.ProductAuthor;
import team.startup.gwangsan.application.post.data.ProductDetailView;
import team.startup.gwangsan.application.post.data.ProductView;
import team.startup.gwangsan.application.post.port.out.BlockPolicyPort;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductPlaceMismatchException;
import team.startup.gwangsan.domain.post.model.Product;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 조회 유스케이스")
class ProductQueryServiceTest {

    @Mock ProductPort productPort;
    @Mock ProductImagePort productImagePort;
    @Mock ProductMemberPort memberPort;
    @Mock BlockPolicyPort blockPolicyPort;
    @Mock ProductChatPort chatPort;

    private static Product product(Long id, Long authorId, ProductStatus status) {
        return new Product(id, authorId, Type.OBJECT, Mode.GIVER, "제목" + id, "설명", 5000, status);
    }

    @Nested
    @DisplayName("상세 조회는")
    class Describe_detail {

        private ProductDetailQueryService service() {
            return new ProductDetailQueryService(productPort, productImagePort, memberPort, blockPolicyPort, chatPort);
        }

        @Test
        @DisplayName("같은 지점이면 작성자·이미지·완료 가능 여부와 상태 플래그를 돌려준다")
        void it_returns_detail() {
            when(memberPort.placeIdOf(1L)).thenReturn(5);
            when(memberPort.placeIdOf(2L)).thenReturn(5);
            when(productPort.findActive(7L)).thenReturn(Optional.of(product(7L, 2L, ProductStatus.RESERVATION)));
            when(memberPort.profile(2L)).thenReturn(new AuthorProfile(2L, "작성자", "광산", 35));
            when(chatPort.findRoom(7L, 1L)).thenReturn(Optional.of(new ChatRoomView(9L, 7L, 1L, 2L)));
            when(chatPort.hasSentMessage(9L, 1L)).thenReturn(true);
            when(productImagePort.findImages(7L)).thenReturn(List.of(new ImageView(3L, "url")));

            ProductDetailView view = service().query(1L, 7L);

            assertThat(view.member()).isEqualTo(new ProductAuthor(2L, "작성자", "광산", 3));
            assertThat(view.images()).containsExactly(new ImageView(3L, "url"));
            assertThat(view.mine()).isFalse();
            assertThat(view.completable()).isTrue();
            assertThat(view.reserved()).isTrue();
            assertThat(view.completed()).isFalse();
            InOrder order = inOrder(memberPort, productPort, blockPolicyPort);
            order.verify(memberPort).placeIdOf(1L);
            order.verify(productPort).findActive(7L);
            order.verify(blockPolicyPort).validateNotBlocked(1L, 2L);
            order.verify(memberPort).placeIdOf(2L);
        }

        @Test
        @DisplayName("작성자 본인이고 채팅방이 없으면 isMine=true, isCompletable=false")
        void it_marks_mine_without_room() {
            when(memberPort.placeIdOf(2L)).thenReturn(5);
            when(productPort.findActive(7L)).thenReturn(Optional.of(product(7L, 2L, ProductStatus.COMPLETED)));
            when(memberPort.profile(2L)).thenReturn(new AuthorProfile(2L, "작성자", "광산", 0));
            when(chatPort.findRoom(7L, 2L)).thenReturn(Optional.empty());

            ProductDetailView view = service().query(2L, 7L);

            assertThat(view.mine()).isTrue();
            assertThat(view.completable()).isFalse();
            assertThat(view.completed()).isTrue();
            assertThat(view.member().light()).isEqualTo(1);
            verify(chatPort, never()).hasSentMessage(anyLong(), anyLong());
        }

        @Test
        @DisplayName("지점이 다르면 PLACE_MISMATCH, 없는 게시글은 NOT_FOUND_PRODUCT, 차단 예외는 그대로 전파한다")
        void it_rejects() {
            when(memberPort.placeIdOf(1L)).thenReturn(5);
            when(productPort.findActive(8L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().query(1L, 8L)).isInstanceOf(NotFoundProductException.class);

            when(productPort.findActive(7L)).thenReturn(Optional.of(product(7L, 2L, ProductStatus.ONGOING)));
            when(memberPort.placeIdOf(2L)).thenReturn(6);
            when(memberPort.profile(2L)).thenReturn(new AuthorProfile(2L, "작성자", "다른 지점", 10));
            assertThatThrownBy(() -> service().query(1L, 7L)).isInstanceOf(ProductPlaceMismatchException.class);

            IllegalStateException blocked = new IllegalStateException("blocked");
            doThrow(blocked).when(blockPolicyPort).validateNotBlocked(1L, 2L);
            assertThatThrownBy(() -> service().query(1L, 7L)).isSameAs(blocked);
        }
    }

    @Nested
    @DisplayName("목록 조회는")
    class Describe_lists {

        private ProductListQueryService service() {
            return new ProductListQueryService(productPort, productImagePort, memberPort);
        }

        @Test
        @DisplayName("지점 목록은 진행 중 게시글에 작성자별 프로필과 이미지를 붙인다")
        void it_lists_place_products() {
            when(memberPort.placeIdOf(1L)).thenReturn(5);
            when(productPort.findOngoingInPlace(Type.OBJECT, null, 5)).thenReturn(List.of(
                    product(7L, 2L, ProductStatus.ONGOING), product(8L, 3L, ProductStatus.ONGOING),
                    product(9L, 2L, ProductStatus.ONGOING)));
            when(memberPort.profiles(List.of(2L, 3L))).thenReturn(Map.of(
                    2L, new AuthorProfile(2L, "둘", "광산", 20), 3L, new AuthorProfile(3L, "셋", "광산", 5)));
            when(productImagePort.findImages(List.of(7L, 8L, 9L))).thenReturn(Map.of(7L, List.of(new ImageView(1L, "a"))));

            List<ProductView> views = service().query(1L, Type.OBJECT, null);

            assertThat(views).extracting(ProductView::id).containsExactly(7L, 8L, 9L);
            assertThat(views.get(0).member().light()).isEqualTo(2);
            assertThat(views.get(1).member().nickname()).isEqualTo("셋");
            assertThat(views.get(1).member().light()).isEqualTo(1);
            assertThat(views.get(0).images()).hasSize(1);
            assertThat(views.get(1).images()).isEmpty();
        }

        @Test
        @DisplayName("비어 있으면 작성자·이미지를 조회하지 않는다")
        void it_returns_empty_without_enrichment() {
            when(memberPort.placeIdOf(1L)).thenReturn(5);
            when(productPort.findOngoingInPlace(null, null, 5)).thenReturn(List.of());

            assertThat(service().query(1L, null, null)).isEmpty();
            verify(memberPort, never()).profiles(any());
            verify(productImagePort, never()).findImages(any(List.class));
        }

        @Test
        @DisplayName("회원 목록은 size 가 없으면 노출 상태 전체, 있으면 cursor 페이지를 쓴다")
        void it_chooses_query_by_size() {
            when(memberPort.profile(2L)).thenReturn(new AuthorProfile(2L, "둘", "광산", 30));
            when(productPort.findByAuthor(2L, null, Mode.GIVER,
                    List.of(ProductStatus.ONGOING, ProductStatus.COMPLETED, ProductStatus.RESERVATION)))
                    .thenReturn(List.of(product(7L, 2L, ProductStatus.COMPLETED)));
            when(productPort.findAuthorPage(2L, null, null, 9L, 2, false))
                    .thenReturn(List.of(product(8L, 2L, ProductStatus.RESERVATION)));
            when(productImagePort.findImages(any(List.class))).thenReturn(Map.of());

            List<ProductView> all = service().query(new MemberProductsCriteria(2L, null, Mode.GIVER, null, null, null));
            List<ProductView> page = service().query(new MemberProductsCriteria(2L, null, null, 9L, 2, false));

            assertThat(all).singleElement().satisfies(v -> {
                assertThat(v.completed()).isTrue();
                assertThat(v.member()).isEqualTo(new ProductAuthor(2L, "둘", "광산", 3));
            });
            assertThat(page).singleElement().satisfies(v -> assertThat(v.reserved()).isTrue());
        }

        @Test
        @DisplayName("회원 상세가 없으면 member 쪽 예외를 그대로 전파한다")
        void it_propagates_missing_member_detail() {
            IllegalStateException missing = new IllegalStateException("NOT_FOUND_MEMBER_DETAIL");
            when(memberPort.profile(2L)).thenThrow(missing);

            assertThatThrownBy(() -> service().query(new MemberProductsCriteria(2L, null, null, null, null, null)))
                    .isSameAs(missing);
            verify(productPort, never()).findByAuthor(anyLong(), any(), any(), any());
        }
    }
}
