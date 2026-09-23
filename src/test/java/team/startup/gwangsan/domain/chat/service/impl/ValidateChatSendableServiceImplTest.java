package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.exception.InvalidImageIdsException;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.security.exception.InvalidMemberPrincipalException;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateChatSendableServiceImpl 단위 테스트")
class ValidateChatSendableServiceImplTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private BlockValidator blockValidator;
    @Mock private ImageRepository imageRepository;

    @InjectMocks
    private ValidateChatSendableServiceImpl service;

    @Nested
    @DisplayName("이미지 메타데이터 검증은")
    class Describe_images {

        @Test
        @DisplayName("인증 실패 시 방과 이미지를 조회하지 않는다")
        void it_unauthenticated_request_never_reads_room_or_images() {
            when(memberUtil.getCurrentMember()).thenThrow(
                    new InvalidMemberPrincipalException());
            assertThatThrownBy(() -> service.execute(10L, List.of("11")))
                    .isInstanceOf(InvalidMemberPrincipalException.class);
            verifyNoInteractions(chatRoomRepository, blockValidator, imageRepository);
        }

        @Test
        @DisplayName("DB 반환 순서와 무관하게 요청 순서와 중복을 보존한다")
        void it_returns_images_in_requested_order_with_duplicates() {
            allowRoom();
            Image first = mock(Image.class);
            Image second = mock(Image.class);
            when(first.getId()).thenReturn(11L);
            when(first.getImageUrl()).thenReturn("https://example.com/11.jpg");
            when(second.getId()).thenReturn(12L);
            when(second.getImageUrl()).thenReturn("https://example.com/12.jpg");
            List<Long> ids = List.of(12L, 11L, 12L);
            when(imageRepository.findAllById(ids)).thenReturn(List.of(first, second));

            assertThat(service.execute(10L, List.of("12", "11", "12")).images()).containsExactly(
                    new GetImageResponse(12L, "https://example.com/12.jpg"),
                    new GetImageResponse(11L, "https://example.com/11.jpg"),
                    new GetImageResponse(12L, "https://example.com/12.jpg"));
        }

        @Test
        @DisplayName("이미지가 전부 없으면 404 예외를 던진다")
        void it_rejects_missing_images_without_partial_response() {
            allowRoom();
            when(imageRepository.findAllById(List.of(11L))).thenReturn(List.of());
            assertThatThrownBy(() -> service.execute(10L, List.of("11")))
                    .isInstanceOf(ImageNotFoundException.class);
        }

        @Test
        @DisplayName("일부 이미지가 없으면 전체 요청이 실패한다")
        void it_rejects_partially_missing_images() {
            allowRoom();
            Image image = mock(Image.class);
            when(image.getId()).thenReturn(11L);
            when(imageRepository.findAllById(List.of(11L, 12L))).thenReturn(List.of(image));

            assertThatThrownBy(() -> service.execute(10L, List.of("11", "12")))
                    .isInstanceOf(ImageNotFoundException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1", "9007199254740991", "00011"})
        @DisplayName("양의 안전 정수 경계와 선행 0을 허용한다")
        void it_accepts_safe_integer_boundaries(String value) {
            allowRoom();
            long id = Long.parseLong(value);
            Image image = mock(Image.class);
            when(image.getId()).thenReturn(id);
            when(image.getImageUrl()).thenReturn("https://example.com/image.jpg");
            when(imageRepository.findAllById(List.of(id))).thenReturn(List.of(image));

            assertThat(service.execute(10L, List.of(value)).images())
                    .containsExactly(new GetImageResponse(id, "https://example.com/image.jpg"));
            var order = inOrder(blockValidator, imageRepository);
            order.verify(blockValidator).validate(any(Member.class), nullable(Member.class));
            order.verify(imageRepository).findAllById(List.of(id));
        }

        @Test
        @DisplayName("빈 목록과 null 요소는 조회 전에 거부한다")
        void it_rejects_empty_list_and_null_element() {
            allowRoom();
            assertThatThrownBy(() -> service.execute(10L, List.of()))
                    .isInstanceOf(InvalidImageIdsException.class);
            assertThatThrownBy(() -> service.execute(10L, java.util.Arrays.asList("11", null)))
                    .isInstanceOf(InvalidImageIdsException.class);
            verifyNoInteractions(imageRepository);
        }

    }

    private void allowRoom() {

        Member member = mock(Member.class);
        ChatRoom room = mock(ChatRoom.class);
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(room));
        when(room.isParticipant(member)).thenReturn(true);
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("채팅방이 존재하지 않으면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_room_not_found() {
            when(memberUtil.getCurrentMember()).thenReturn(mock(Member.class));
            when(chatRoomRepository.findChatRoomByRoomId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L, List.of("11")))
                    .isInstanceOf(NotFoundChatRoomException.class);

            verifyNoInteractions(blockValidator, imageRepository);
        }

        @Test
        @DisplayName("요청자가 채팅방의 참여자가 아니면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_member_is_not_participant() {
            Member member = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatRoom.isParticipant(member)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(10L, List.of("11")))
                    .isInstanceOf(NotFoundChatRoomException.class);

            verifyNoInteractions(blockValidator, imageRepository);
        }

        @Test
        @DisplayName("상대방과 차단 관계면 BlockedMemberException 을 던진다")
        void it_throws_BlockedMemberException_when_blocked() {
            Member member = mock(Member.class);
            Member otherMember = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatRoom.isParticipant(member)).thenReturn(true);
            when(chatRoom.getOtherMember(member)).thenReturn(otherMember);
            doThrow(new BlockedMemberException()).when(blockValidator).validate(member, otherMember);

            assertThatThrownBy(() -> service.execute(10L, List.of("11")))
                    .isInstanceOf(BlockedMemberException.class);
            verifyNoInteractions(imageRepository);
        }

        @Test
        @DisplayName("참여자이고 차단 관계가 없으면 예외 없이 통과한다")
        void it_passes_when_participant_and_not_blocked() {
            Member member = mock(Member.class);
            Member otherMember = mock(Member.class);
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(memberUtil.getCurrentMember()).thenReturn(member);
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatRoom.isParticipant(member)).thenReturn(true);
            when(chatRoom.getOtherMember(member)).thenReturn(otherMember);

            assertThat(service.execute(10L, null)).isNull();
            verifyNoInteractions(imageRepository);

            verify(blockValidator).validate(member, otherMember);
        }
    }
}
