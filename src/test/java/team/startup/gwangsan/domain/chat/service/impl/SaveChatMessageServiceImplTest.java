package team.startup.gwangsan.domain.chat.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.SaveChatMessageResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageCustomRepository.StoredMessage;
import team.startup.gwangsan.domain.chat.exception.ChatMessageIdConflictException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatMessageException;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.global.util.BlockValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    @Mock private BlockValidator blockValidator;

    @InjectMocks
    private SaveChatMessageServiceImpl service;

    @Nested
    @DisplayName("수신자의 나가기 및 재참여 상태에 따른 알림")
    class RecipientVisibility {

        @ParameterizedTest
        @CsvSource({"true,true", "false,true", "true,false", "false,false"})
        @DisplayName("양방향에서 재참여 전까지 숨김과 푸시 차단을 유지하고 재참여 후 알림을 보낸다")
        void it_notifies_only_previously_visible_recipient(boolean senderIsBuyer, boolean recipientHidden) {
            Member buyer = mock(Member.class);
            Member seller = mock(Member.class);
            when(buyer.getId()).thenReturn(1L);
            when(seller.getId()).thenReturn(2L);
            Member sender = senderIsBuyer ? buyer : seller;
            Member recipient = senderIsBuyer ? seller : buyer;
            ChatRoom room = ChatRoom.builder().buyer(buyer).seller(seller).build();
            ReflectionTestUtils.setField(room, "id", 10L);
            LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
            room.hideFor(sender, now);
            if (recipientHidden) {
                room.hideFor(recipient, now);
            }
            when(memberRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(room));
            when(chatMessageRepository.insertIfAbsent(any())).thenReturn(true);
            when(chatMessageRepository.getReferenceById(1L)).thenReturn(mock(ChatMessage.class));
            Image image = mock(Image.class);
            when(image.getId()).thenReturn(100L);
            when(image.getImageUrl()).thenReturn("image-url");
            when(imageRepository.findAllById(List.of(100L))).thenReturn(List.of(image));
            DeviceToken token = mock(DeviceToken.class);
            if (!recipientHidden) {
                when(deviceTokenRepository.findAllByUserId(recipient.getId())).thenReturn(List.of(token));
            }

            SaveChatMessageResponse response = service.execute(
                    1L, 10L, "이미지", List.of(100L), MessageType.IMAGE, sender.getId(), now);

            verify(chatMessageRepository).insertIfAbsent(any(ChatMessage.class));
            verify(chatMessageImageRepository).saveAll(anyList());
            assertThat(response.images()).hasSize(1);
            assertThat(response.images().getFirst().imageId()).isEqualTo(100L);
            assertThat(room.isHiddenFor(sender)).isTrue();
            assertThat(room.isHiddenFor(recipient)).isEqualTo(recipientHidden);
            if (recipientHidden) {
                verifyNoInteractions(deviceTokenRepository, applicationEventPublisher);
                service.execute(2L, 10L, "다음 메시지", null, MessageType.TEXT, sender.getId(), now);
                verify(chatMessageRepository, times(2)).insertIfAbsent(any(ChatMessage.class));
                verifyNoInteractions(deviceTokenRepository, applicationEventPublisher);
                assertThat(room.isHiddenFor(recipient)).isTrue();

                MemberUtil memberUtil = mock(MemberUtil.class);
                ProductRepository productRepository = mock(ProductRepository.class);
                Product product = mock(Product.class);
                when(memberUtil.getCurrentMember()).thenReturn(recipient);
                when(productRepository.findActiveById(100L)).thenReturn(Optional.of(product));
                when(product.getMember()).thenReturn(sender);
                when(product.getMode()).thenReturn(senderIsBuyer ? Mode.RECEIVER : Mode.GIVER);
                when(chatRoomRepository.findByProductIdAndBuyerAndSeller(100L, buyer, seller))
                        .thenReturn(Optional.of(room));
                CreateChatRoomServiceImpl createService = new CreateChatRoomServiceImpl(
                        chatRoomRepository, memberUtil, productRepository, blockValidator);

                assertThat(createService.execute(100L).roomId()).isEqualTo(10L);
                verify(chatRoomRepository, never()).save(any());
                assertThat(room.isHiddenFor(recipient)).isFalse();
                assertThat(room.isHiddenFor(sender)).isTrue();
                when(deviceTokenRepository.findAllByUserId(recipient.getId())).thenReturn(List.of(token));
                service.execute(3L, 10L, "재참여 후 메시지", null, MessageType.TEXT, sender.getId(), now);
                verify(deviceTokenRepository).findAllByUserId(recipient.getId());
                verify(applicationEventPublisher).publishEvent(
                        new SendNotificationEvent(List.of(token), NotificationType.CHATTING, 10L));
            } else {
                verify(deviceTokenRepository).findAllByUserId(recipient.getId());
                verify(applicationEventPublisher).publishEvent(
                        new SendNotificationEvent(List.of(token), NotificationType.CHATTING, 10L));
            }
        }
    }

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
            when(chatRoom.getOtherMember(sender)).thenReturn(otherMember);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatMessageRepository.insertIfAbsent(any())).thenReturn(true);
            when(deviceTokenRepository.findAllByUserId(any())).thenReturn(List.of());
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
            verify(chatMessageRepository).insertIfAbsent(captor.capture());
            assertThat(captor.getValue().getContent()).isEqualTo("안녕하세요");
            assertThat(captor.getValue().getMessageType()).isEqualTo(MessageType.TEXT);
            assertThat(captor.getValue().getChecked()).isFalse();
            assertThat(response).isNotNull();
            verifyNoInteractions(imageRepository);
            verifyNoInteractions(chatMessageImageRepository);
        }

        @Test
        @DisplayName("새 메시지가 와도 참여자의 숨김을 해제하지 않는다")
        void it_does_not_unhide_room_when_message_arrives() {
            arrangeDefaultScenario();

            service.execute(1L, 10L, "안녕하세요", null, MessageType.TEXT, 1L, now);

            verify(chatRoom, never()).unhideFor(any());
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
            when(chatMessageRepository.getReferenceById(1L)).thenReturn(mock(ChatMessage.class));
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
            when(chatMessageRepository.getReferenceById(1L)).thenReturn(mock(ChatMessage.class));
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
            DeviceToken token1 = mock(DeviceToken.class);
            DeviceToken token2 = mock(DeviceToken.class);
            when(deviceTokenRepository.findAllByUserId(2L)).thenReturn(List.of(token1, token2));

            service.execute(1L, 10L, "안녕", null, MessageType.TEXT, 1L, now);

            ArgumentCaptor<SendNotificationEvent> captor = ArgumentCaptor.forClass(SendNotificationEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(NotificationType.CHATTING);
            assertThat(captor.getValue().sourceId()).isEqualTo(10L);
            assertThat(captor.getValue().deviceTokens()).containsExactly(token1, token2);
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

            verify(deviceTokenRepository).findAllByUserId(2L);
        }

        @Test
        @DisplayName("sender 가 seller 이면 buyer 에게 알림을 보낸다")
        void it_sends_notification_to_buyer_when_sender_is_seller() {
            Member sellerMember = mock(Member.class);
            Member buyerMember = mock(Member.class);
            ChatRoom room = mock(ChatRoom.class);

            when(sellerMember.getId()).thenReturn(3L);
            when(buyerMember.getId()).thenReturn(4L);
            when(room.getOtherMember(sellerMember)).thenReturn(buyerMember);
            when(memberRepository.findById(3L)).thenReturn(Optional.of(sellerMember));
            when(chatRoomRepository.findChatRoomByRoomId(20L)).thenReturn(Optional.of(room));
            when(chatMessageRepository.insertIfAbsent(any())).thenReturn(true);
            when(deviceTokenRepository.findAllByUserId(any())).thenReturn(List.of());

            service.execute(1L, 20L, "안녕", null, MessageType.TEXT, 3L, now);

            verify(deviceTokenRepository).findAllByUserId(4L);
        }

        @Test
        @DisplayName("차단 관계면 예외를 던지고 이미지와 알림을 처리하지 않는다")
        void it_throws_BlockedMemberException_when_blocked() {
            when(chatRoom.getOtherMember(sender)).thenReturn(otherMember);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(chatRoomRepository.findChatRoomByRoomId(10L)).thenReturn(Optional.of(chatRoom));
            when(chatMessageRepository.insertIfAbsent(any())).thenReturn(true);
            doThrow(new BlockedMemberException()).when(blockValidator).validate(sender, otherMember);

            assertThatThrownBy(() -> service.execute(1L, 10L, "안녕", null, MessageType.TEXT, 1L, now))
                    .isInstanceOf(BlockedMemberException.class);

            verify(chatMessageRepository, never()).save(any());
            verifyNoInteractions(chatMessageImageRepository);
            verifyNoInteractions(deviceTokenRepository);
            verifyNoInteractions(applicationEventPublisher);
        }
    }

    @Nested
    @DisplayName("재전송 메시지는")
    class Describe_replay {

        private static final Long MESSAGE_ID = 1L;
        private static final Long ROOM_ID = 10L;
        private static final Long SENDER_ID = 20L;
        private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 12, 0, 0, 123_456_000);

        private Member sender;
        private Member recipient;
        private ChatRoom room;

        @BeforeEach
        void setUp() {
            sender = member(SENDER_ID, "발신자");
            recipient = member(30L, "수신자");
            room = room(ROOM_ID, sender, recipient);
        }

        @Test
        @DisplayName("이미 존재하는 메시지는 같은 저장 응답을 반환하고 저장·차단·알림을 다시 실행하지 않는다")
        void it_replays_existing_message_without_repeating_side_effects() {
            StoredMessage stored = storedMessage(MessageType.TEXT, CREATED_AT, List.of());
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.of(stored));

            SaveChatMessageResponse response = service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", null, MessageType.TEXT, SENDER_ID, CREATED_AT);

            assertThat(response).isEqualTo(responseOf(stored));
            verify(chatMessageRepository, never()).insertIfAbsent(any());
            verify(chatMessageRepository, never()).save(any());
            verifyNoInteractions(memberRepository, chatRoomRepository, imageRepository, chatMessageImageRepository,
                    blockValidator, deviceTokenRepository, applicationEventPublisher);
        }

        @Test
        @DisplayName("삽입 경쟁에서 저장소가 false를 반환하면 같은 저장 응답을 반환하고 후속 부작용을 실행하지 않는다")
        void it_replays_message_after_insert_race_without_repeating_side_effects() {
            StoredMessage stored = storedMessage(MessageType.TEXT, CREATED_AT, List.of());
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(false);
            when(memberRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
            when(chatRoomRepository.findChatRoomByRoomId(ROOM_ID)).thenReturn(Optional.of(room));
            when(chatMessageRepository.insertIfAbsent(any(ChatMessage.class))).thenReturn(false);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.of(stored));

            SaveChatMessageResponse response = service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", null, MessageType.TEXT, SENDER_ID, CREATED_AT);

            assertThat(response).isEqualTo(responseOf(stored));
            verify(chatMessageRepository).insertIfAbsent(any(ChatMessage.class));
            verify(chatMessageRepository, never()).save(any());
            verifyNoInteractions(imageRepository, chatMessageImageRepository, blockValidator,
                    deviceTokenRepository, applicationEventPublisher);
        }

        @Test
        @DisplayName("존재한다고 판단한 메시지의 저장 정보가 없으면 NotFoundChatMessageException 을 던진다")
        void it_throws_NotFoundChatMessageException_when_replayed_message_is_not_found() {
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", null, MessageType.TEXT, SENDER_ID, CREATED_AT))
                    .isInstanceOf(NotFoundChatMessageException.class);

            verifyNoInteractions(memberRepository, chatRoomRepository, imageRepository, chatMessageImageRepository,
                    blockValidator, deviceTokenRepository, applicationEventPublisher);
        }

        @DisplayName("방·발신자·내용·유형·밀리초 timestamp 중 하나가 다르면 ChatMessageIdConflictException 을 던진다")
        @Test
        void it_throws_ChatMessageIdConflictException_when_replay_payload_differs() {
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID))
                    .thenReturn(Optional.of(storedMessage(MessageType.TEXT, CREATED_AT, List.of())));

            assertConflict(ROOM_ID + 1, "내용", MessageType.TEXT, SENDER_ID, CREATED_AT);
            assertConflict(ROOM_ID, "내용", MessageType.TEXT, SENDER_ID + 1, CREATED_AT);
            assertConflict(ROOM_ID, "다른 내용", MessageType.TEXT, SENDER_ID, CREATED_AT);
            assertConflict(ROOM_ID, "내용", MessageType.IMAGE, SENDER_ID, CREATED_AT);
            assertConflict(ROOM_ID, "내용", MessageType.TEXT, SENDER_ID, CREATED_AT.plusNanos(1_000_000));
        }

        @Test
        @DisplayName("같은 밀리초 안의 timestamp 차이는 같은 메시지로 재전송한다")
        void it_replays_when_timestamp_differs_only_below_millisecond() {
            StoredMessage stored = storedMessage(MessageType.TEXT, CREATED_AT, List.of());
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.of(stored));
            LocalDateTime withinSameMillisecond = LocalDateTime.of(2024, 1, 1, 12, 0, 0, 123_999_999);

            SaveChatMessageResponse response = service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", null, MessageType.TEXT, SENDER_ID, withinSameMillisecond);

            assertThat(response).isEqualTo(responseOf(stored));
        }

        @Test
        @DisplayName("IMAGE 재전송에서 null imageIds 와 빈 저장 이미지 목록은 같다")
        void it_replays_image_message_when_image_ids_are_null_and_stored_images_are_empty() {
            StoredMessage stored = storedMessage(MessageType.IMAGE, CREATED_AT, List.of());
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.of(stored));

            SaveChatMessageResponse response = service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", null, MessageType.IMAGE, SENDER_ID, CREATED_AT);

            assertThat(response).isEqualTo(responseOf(stored));
            verify(chatMessageRepository, never()).findExistingImageIds(any());
        }

        @Test
        @DisplayName("IMAGE 재전송에서 빈 imageIds 와 빈 저장 이미지 목록은 같다")
        void it_replays_image_message_when_image_ids_are_empty_and_stored_images_are_empty() {
            StoredMessage stored = storedMessage(MessageType.IMAGE, CREATED_AT, List.of());
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.of(stored));
            when(chatMessageRepository.findExistingImageIds(List.of())).thenReturn(Set.of());

            SaveChatMessageResponse response = service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", List.of(), MessageType.IMAGE, SENDER_ID, CREATED_AT);

            assertThat(response).isEqualTo(responseOf(stored));
        }

        @Test
        @DisplayName("IMAGE 재전송은 존재하는 이미지 ID 집합이 순서와 중복에 관계없이 같으면 저장 응답을 반환한다")
        void it_replays_image_message_when_existing_image_id_sets_are_equal() {
            List<GetImageResponse> images = List.of(new GetImageResponse(100L, "first"), new GetImageResponse(200L, "second"));
            StoredMessage stored = storedMessage(MessageType.IMAGE, CREATED_AT, images);
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.of(stored));
            when(chatMessageRepository.findExistingImageIds(List.of(200L, 100L, 100L))).thenReturn(Set.of(100L, 200L));

            SaveChatMessageResponse response = service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", List.of(200L, 100L, 100L), MessageType.IMAGE, SENDER_ID, CREATED_AT);

            assertThat(response).isEqualTo(responseOf(stored));
        }

        @Test
        @DisplayName("IMAGE 재전송의 존재하는 이미지 ID 집합이 다르면 ChatMessageIdConflictException 을 던진다")
        void it_throws_ChatMessageIdConflictException_when_existing_image_id_sets_differ() {
            StoredMessage stored = storedMessage(MessageType.IMAGE, CREATED_AT, List.of(new GetImageResponse(100L, "first")));
            when(chatMessageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(chatMessageRepository.findStoredMessage(MESSAGE_ID)).thenReturn(Optional.of(stored));
            when(chatMessageRepository.findExistingImageIds(List.of(100L, 200L))).thenReturn(Set.of(100L, 200L));

            assertThatThrownBy(() -> service.execute(
                    MESSAGE_ID, ROOM_ID, "내용", List.of(100L, 200L), MessageType.IMAGE, SENDER_ID, CREATED_AT))
                    .isInstanceOf(ChatMessageIdConflictException.class);
        }

        private void assertConflict(Long roomId, String content, MessageType messageType, Long senderId, LocalDateTime createdAt) {
            assertThatThrownBy(() -> service.execute(
                    MESSAGE_ID, roomId, content, null, messageType, senderId, createdAt))
                    .isInstanceOf(ChatMessageIdConflictException.class);
        }

        private static Member member(Long id, String nickname) {
            Member member = Member.builder()
                    .name(nickname).nickname(nickname).phoneNumber(id + "0000000000").password("password").build();
            ReflectionTestUtils.setField(member, "id", id);
            return member;
        }

        private static ChatRoom room(Long id, Member buyer, Member seller) {
            ChatRoom room = ChatRoom.builder().buyer(buyer).seller(seller).build();
            ReflectionTestUtils.setField(room, "id", id);
            return room;
        }

        private static StoredMessage storedMessage(MessageType messageType, LocalDateTime createdAt, List<GetImageResponse> images) {
            return new StoredMessage(MESSAGE_ID, ROOM_ID, SENDER_ID, "내용", messageType, createdAt, false, images);
        }

        private static SaveChatMessageResponse responseOf(StoredMessage stored) {
            return new SaveChatMessageResponse(
                    stored.messageId(), stored.images(), stored.createdAt(), stored.senderId(), stored.checked());
        }
    }
}
