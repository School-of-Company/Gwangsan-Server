package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomProductDto;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomsResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindRoomsByCurrentUserServiceImpl 단위 테스트")
class FindRoomsByCurrentUserServiceImplTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private FindRoomsByCurrentUserServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        private Member member;

        @BeforeEach
        void setUp() {
            member = mock(Member.class);
            when(member.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(member);
        }

        @Test
        @DisplayName("방 목록이 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_rooms() {
            when(chatRoomRepository.findRoomsByMemberId(1L)).thenReturn(List.of());
            when(productRepository.findRoomProductsWithImagesByIds(Set.of())).thenReturn(List.of());

            List<GetRoomsResponse> result = service.execute();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("방 목록을 올바른 상품 정보와 매핑하여 반환한다")
        void it_returns_rooms_mapped_with_correct_product() {
            GetRoomsDto roomDto = new GetRoomsDto(
                    10L, null, 1L, "마지막 메시지", MessageType.TEXT,
                    LocalDateTime.now(), 0L, 100L
            );
            GetRoomProductDto productDto = new GetRoomProductDto(100L, "상품명", List.of());

            when(chatRoomRepository.findRoomsByMemberId(1L)).thenReturn(List.of(roomDto));
            when(productRepository.findRoomProductsWithImagesByIds(Set.of(100L))).thenReturn(List.of(productDto));

            List<GetRoomsResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).roomId()).isEqualTo(10L);
            assertThat(result.get(0).lastMessage()).isEqualTo("마지막 메시지");
            assertThat(result.get(0).product()).isSameAs(productDto);
        }

        @Test
        @DisplayName("여러 방이 각각 올바른 상품에 매핑된다")
        void it_maps_each_room_to_correct_product() {
            GetRoomsDto room1 = new GetRoomsDto(1L, null, 1L, "msg1", MessageType.TEXT, LocalDateTime.now(), 0L, 100L);
            GetRoomsDto room2 = new GetRoomsDto(2L, null, 2L, "msg2", MessageType.TEXT, LocalDateTime.now(), 1L, 200L);
            GetRoomProductDto product1 = new GetRoomProductDto(100L, "상품1", List.of());
            GetRoomProductDto product2 = new GetRoomProductDto(200L, "상품2", List.of());

            when(chatRoomRepository.findRoomsByMemberId(1L)).thenReturn(List.of(room1, room2));
            when(productRepository.findRoomProductsWithImagesByIds(Set.of(100L, 200L)))
                    .thenReturn(List.of(product1, product2));

            List<GetRoomsResponse> result = service.execute();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).product().productId()).isEqualTo(100L);
            assertThat(result.get(0).product().title()).isEqualTo("상품1");
            assertThat(result.get(1).product().productId()).isEqualTo(200L);
            assertThat(result.get(1).product().title()).isEqualTo("상품2");
        }

        @Test
        @DisplayName("상품이 삭제되어 productDtoMap 에 없으면 product 가 null 인 응답이 반환된다")
        void it_returns_null_product_when_product_deleted() {
            GetRoomsDto roomDto = new GetRoomsDto(
                    10L, null, 1L, "메시지", MessageType.TEXT,
                    LocalDateTime.now(), 0L, 999L
            );

            when(chatRoomRepository.findRoomsByMemberId(1L)).thenReturn(List.of(roomDto));
            when(productRepository.findRoomProductsWithImagesByIds(Set.of(999L))).thenReturn(List.of());

            List<GetRoomsResponse> result = service.execute();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).product()).isNull();
        }
    }
}
