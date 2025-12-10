package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductByIdResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindProductByIdServiceImpl 단위 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
class FindProductByIdServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private FindProductByIdServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("정상 케이스에서 상품 상세 정보를 반환하고 isMine=false, isCompletable=true, isCompleted=false 를 설정한다")
        void it_returns_product_detail_with_flags_for_normal_case() {
            // given
            Long productId = 1L;

            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(100L);
            when(currentMember.getNickname()).thenReturn("현재 유저");

            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            Place myPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(currentMember.getId()))
                    .thenReturn(myPlace);

            Member owner = mock(Member.class);
            when(owner.getId()).thenReturn(200L);
            when(owner.getNickname()).thenReturn("판매자");

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getTitle()).thenReturn("테스트 상품");
            when(product.getDescription()).thenReturn("설명");
            when(product.getGwangsan()).thenReturn(10);
            when(product.getType()).thenReturn(Type.SERVICE);
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMember()).thenReturn(owner);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            Place productPlace = mock(Place.class);
            when(productPlace.getName()).thenReturn("광산구");
            when(memberDetailRepository.findPlaceByMemberId(owner.getId()))
                    .thenReturn(productPlace);
            when(productPlace.isSamePlace(myPlace)).thenReturn(true);

            MemberDetail ownerDetail = mock(MemberDetail.class);
            when(ownerDetail.getId()).thenReturn(300L);
            when(ownerDetail.getMember()).thenReturn(owner);
            when(ownerDetail.getPlace()).thenReturn(productPlace);
            when(ownerDetail.getLight()).thenReturn(25); // light => 2
            when(memberDetailRepository.findById(owner.getId()))
                    .thenReturn(Optional.of(ownerDetail));

            Image image = mock(Image.class);
            when(image.getId()).thenReturn(11L);
            when(image.getImageUrl()).thenReturn("https://example.com/image.png");

            ProductImage productImage = mock(ProductImage.class);
            when(productImage.getProduct()).thenReturn(product);
            when(productImage.getImage()).thenReturn(image);

            when(productImageRepository.findAllByProductId(productId))
                    .thenReturn(List.of(productImage));

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoomRepository.findByProductIdAndMember(productId, currentMember))
                    .thenReturn(Optional.ofNullable(chatRoom));
            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, currentMember.getId()))
                    .thenReturn(true);

            // when
            GetProductByIdResponse result = service.execute(productId);

            // then
            assertThat(result.id()).isEqualTo(productId);
            assertThat(result.title()).isEqualTo("테스트 상품");
            assertThat(result.content()).isEqualTo("설명");
            assertThat(result.gwangsan()).isEqualTo(10);
            assertThat(result.type()).isEqualTo(Type.SERVICE);
            assertThat(result.mode()).isEqualTo(Mode.GIVER);

            GetProductMemberResponse memberResponse = result.member();
            assertThat(memberResponse.memberId()).isEqualTo(300L);
            assertThat(memberResponse.nickname()).isEqualTo("판매자");
            assertThat(memberResponse.placeName()).isEqualTo("광산구");
            assertThat(memberResponse.light()).isEqualTo(2); // 25 / 10 => 2

            List<GetImageResponse> images = result.images();
            assertThat(images).hasSize(1);
            assertThat(images.get(0).imageId()).isEqualTo(11L);
            assertThat(images.get(0).imageUrl()).isEqualTo("https://example.com/image.png");

            assertThat(result.isMine()).isFalse();
            assertThat(result.isCompletable()).isTrue();
            assertThat(result.isCompleted()).isFalse();

            verify(productRepository).findById(productId);
            verify(productImageRepository).findAllByProductId(productId);
            verify(chatRoomRepository).findByProductIdAndMember(productId, currentMember);
            verify(chatMessageRepository).existsByRoomAndSenderId(chatRoom, currentMember.getId());
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 NotFoundProductException 을 던진다")
        void it_throws_NotFoundProductException_when_product_not_found() {
            // given
            Long productId = 999L;

            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            Place myPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(currentMember.getId()))
                    .thenReturn(myPlace);

            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundProductException.class,
                    () -> service.execute(productId));

            verify(productRepository).findById(productId);
            verifyNoMoreInteractions(productImageRepository, chatRoomRepository, chatMessageRepository);
        }

        @Test
        @DisplayName("회원과 상품 등록자의 장소가 다르면 PlaceMismatchException 을 던진다")
        void it_throws_PlaceMismatchException_when_place_is_different() {
            // given
            Long productId = 1L;

            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            Place myPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(currentMember.getId()))
                    .thenReturn(myPlace);

            Member owner = mock(Member.class);
            when(owner.getId()).thenReturn(2L);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getMember()).thenReturn(owner);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            Place productPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(owner.getId()))
                    .thenReturn(productPlace);
            when(productPlace.isSamePlace(myPlace)).thenReturn(false);

            MemberDetail ownerDetail = mock(MemberDetail.class);
            when(ownerDetail.getMember()).thenReturn(owner);
            when(ownerDetail.getPlace()).thenReturn(productPlace);
            when(ownerDetail.getLight()).thenReturn(10);
            when(memberDetailRepository.findById(owner.getId()))
                    .thenReturn(Optional.of(ownerDetail));

            // when & then
            assertThrows(PlaceMismatchException.class,
                    () -> service.execute(productId));

            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("현재 유저가 상품 등록자면 isMine=true, isCompleted=true 로 반환한다")
        void it_sets_isMine_true_and_isCompleted_true_when_owner_is_current_user() {
            // given
            Long productId = 1L;

            Member owner = mock(Member.class);
            when(owner.getId()).thenReturn(1L);
            when(owner.getNickname()).thenReturn("나");

            when(memberUtil.getCurrentMember()).thenReturn(owner);

            Place place = mock(Place.class);
            when(place.getName()).thenReturn("광산구");
            when(memberDetailRepository.findPlaceByMemberId(owner.getId()))
                    .thenReturn(place);
            when(place.isSamePlace(place)).thenReturn(true);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getTitle()).thenReturn("내 상품");
            when(product.getDescription()).thenReturn("내 상품 설명");
            when(product.getGwangsan()).thenReturn(30);
            when(product.getType()).thenReturn(Type.SERVICE);
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);
            when(product.getMember()).thenReturn(owner);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            MemberDetail ownerDetail = mock(MemberDetail.class);
            when(ownerDetail.getId()).thenReturn(10L);
            when(ownerDetail.getMember()).thenReturn(owner);
            when(ownerDetail.getPlace()).thenReturn(place);
            when(ownerDetail.getLight()).thenReturn(5);
            when(memberDetailRepository.findById(owner.getId()))
                    .thenReturn(Optional.of(ownerDetail));

            when(productImageRepository.findAllByProductId(productId))
                    .thenReturn(List.of());

            when(chatRoomRepository.findByProductIdAndMember(productId, owner))
                    .thenReturn(Optional.empty());

            // when
            GetProductByIdResponse result = service.execute(productId);

            // then
            assertThat(result.isMine()).isTrue();
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.isCompletable()).isFalse();

            GetProductMemberResponse memberResponse = result.member();
            assertThat(memberResponse.light()).isEqualTo(1);
        }

        @Test
        @DisplayName("상품 등록자의 MemberDetail 이 없으면 NotFoundMemberException 을 던진다")
        void it_throws_NotFoundMemberException_when_owner_detail_not_found() {
            // given
            Long productId = 1L;

            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            Place myPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(currentMember.getId()))
                    .thenReturn(myPlace);

            Member owner = mock(Member.class);
            when(owner.getId()).thenReturn(2L);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getMember()).thenReturn(owner);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            Place productPlace = mock(Place.class);
            when(memberDetailRepository.findPlaceByMemberId(owner.getId()))
                    .thenReturn(productPlace);
            when(productPlace.isSamePlace(myPlace)).thenReturn(true);

            when(memberDetailRepository.findById(owner.getId()))
                    .thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundMemberException.class,
                    () -> service.execute(productId));
        }
    }
}
