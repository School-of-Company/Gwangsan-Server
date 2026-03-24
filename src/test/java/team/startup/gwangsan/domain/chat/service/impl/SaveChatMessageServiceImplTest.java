package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.SaveChatMessageResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.event.SendNotificationEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaveChatMessageServiceImpl 단위 테스트")
class SaveChatMessageServiceImplTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private ChatMessageImageRepository chatMessageImageRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private SaveChatMessageServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        private Member sender;
        private Member otherMember;
        private ChatRoom chatRoom;
        private final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        @BeforeEach
        void setUp() {
            sender = mock(Member.class);
            otherMember = mock(Member.class);
            chatRoom = mock(ChatRoom.class);
        }

        private void arrangeDefaultScenario() {
            when(sender.getId()).thenReturn(1L);
            when(otherMember.getId()).thenReturn(2L);
            when(chatRoom.getBuyer()).thenReturn(sender);
            when(chatRoom.getSeller()).thenReturn(otherMember);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deviceTokenRepository.findByUserId(any())).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("발신자를 찾을 수 없으면 NotFoundMemberException 을 던진다")
        void it_throws_NotFoundMemberException_when_sender_not_found() {
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L, 10L, "내용", null, MessageType.TEXT, 99L, now))
                    .isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("채팅방을 찾을 수 없으면 NotFoundChatRoomException 을 던진다")
        void it_throws_NotFoundChatRoomException_when_room_not_found() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(chatRoomRepository.findChatRoomByRoomId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L, 99L, "내용", null, MessageType.TEXT, 1L, now))
                    .isInstanceOf(NotFoundChatRoomException.class);
        }

        @Test
        @DisplayName("TEXT 메시지를 저장하고 응답을 반환한다")
        void it_saves_text_message_and_returns_response() {
            arrangeDefaultScenario();

            SaveChatMessageResponse response = service.execute(1L, 10L, "안녕하세요", null, MessageType.TEXT, 1L, now);

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());
            assertThat(captor.getValue().getContent()).isEqualTo("안녕하세요");
            assertThat(captor.getValue().getMessageType()).isEqualTo(MessageType.TEXT);
            assertThat(captor.getValue().getChecked()).isFalse();
            assertThat(response).isNotNull();
            verifyNoInteractions(imageRepository);
            verifyNoInteractions(chatMessageImageRepository);
        }

        @Test
        @DisplayName("IMAGE 타입이고 imageIds 가 있으면 이미지를 저장한다")
        void it_saves_images_when_message_type_is_image() {
            arrangeDefaultScenario();
            Image image1 = mock(Image.class);
            Image image2 = mock(Image.class);
            when(image1.getId()).thenReturn(100L);
            when(image1.getImageUrl()).thenReturn("url1");
            when(image2.getId()).thenReturn(200L);
            when(image2.getImageUrl()).thenReturn("url2");
            when(imageRepository.findAllById(List.of(100L, 200L))).thenReturn(List.of(image1, image2));
            when(chatMessageImageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            SaveChatMessageResponse response = service.execute(1L, 10L, "이미지", List.of(100L, 200L), MessageType.IMAGE, 1L, now);

            verify(imageRepository).findAllById(List.of(100L, 200L));
            verify(chatMessageImageRepository).saveAll(anyList());
            assertThat(response.images()).hasSize(2);
        }

        @Test
        @DisplayName("imageIds 일부가 존재하지 않아도 조회된 이미지만 저장한다")
        void it_saves_only_found_images_when_some_ids_not_exist() {
            arrangeDefaultScenario();
            Image image = mock(Image.class);
            when(image.getId()).thenReturn(100L);
            when(image.getImageUrl()).thenReturn("url");
            when(imageRepository.findAllById(List.of(100L, 999L))).thenReturn(List.of(image));
            when(chatMessageImageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            SaveChatMessageResponse response = service.execute(1L, 10L, "이미지", List.of(100L, 999L), MessageType.IMAGE, 1L, now);

            assertThat(response.images()).hasSize(1);
        }

        @Test
        @DisplayName("IMAGE 타입이지만 imageIds 가 null 이면 이미지를 저장하지 않는다")
        void it_skips_image_save_when_image_ids_is_null() {
            arrangeDefaultScenario();

            service.execute(1L, 10L, "이미지", null, MessageType.IMAGE, 1L, now);

            verifyNoInteractions(imageRepository);
            verifyNoInteractions(chatMessageImageRepository);
        }

        @Test
        @DisplayName("IMAGE 타입이지만 imageIds 가 비어있으면 이미지를 저장하지 않는다")
        void it_skips_image_save_when_image_ids_is_empty() {
            arrangeDefaultScenario();

            service.execute(1L, 10L, "이미지", List.of(), MessageType.IMAGE, 1L, now);

            verifyNoInteractions(imageRepository);
            verifyNoInteractions(chatMessageImageRepository);
        }

        @Test
        @DisplayName("상대방 디바이스 토큰이 있으면 CHATTING 타입 알림 이벤트를 발행한다")
        void it_publishes_notification_event_when_device_token_exists() {
            arrangeDefaultScenario();
            DeviceToken token = mock(DeviceToken.class);
            when(deviceTokenRepository.findByUserId(2L)).thenReturn(Optional.of(token));

            service.execute(1L, 10L, "안녕", null, MessageType.TEXT, 1L, now);

            ArgumentCaptor<SendNotificationEvent> captor = ArgumentCaptor.forClass(SendNotificationEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(NotificationType.CHATTING);
            assertThat(captor.getValue().sourceId()).isEqualTo(10L);
            assertThat(captor.getValue().deviceTokens()).containsExactly(token);
        }

        @Test
        @DisplayName("상대방 디바이스 토큰이 없으면 알림 이벤트를 발행하지 않는다")
        void it_does_not_publish_event_when_device_token_not_found() {
            arrangeDefaultScenario();

            service.execute(1L, 10L, "안녕", null, MessageType.TEXT, 1L, now);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("sender 가 buyer 이면 seller 에게 알림을 보낸다")
        void it_sends_notification_to_seller_when_sender_is_buyer() {
            arrangeDefaultScenario();

            service.execute(1L, 10L, "안녕", null, MessageType.TEXT, 1L, now);

            verify(deviceTokenRepository).findByUserId(2L);
        }

        @Test
        @DisplayName("sender 가 seller 이면 buyer 에게 알림을 보낸다")
        void it_sends_notification_to_buyer_when_sender_is_seller() {
            Member sellerMember = mock(Member.class);
            Member buyerMember = mock(Member.class);
            ChatRoom room = mock(ChatRoom.class);

            when(sellerMember.getId()).thenReturn(3L);
            when(buyerMember.getId()).thenReturn(4L);
            when(room.getBuyer()).thenReturn(buyerMember);
            when(memberRepository.findById(3L)).thenReturn(Optional.of(sellerMember));
            when(chatRoomRepository.findChatRoomByRoomId(20L)).thenReturn(Optional.of(room));
            when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deviceTokenRepository.findByUserId(any())).thenReturn(Optional.empty());

            service.execute(1L, 20L, "안녕", null, MessageType.TEXT, 3L, now);

            verify(deviceTokenRepository).findByUserId(4L);
        }
    }
}
