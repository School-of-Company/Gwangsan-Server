package team.startup.gwangsan.adapter.web.chat.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import team.startup.gwangsan.application.chat.data.ChatMessageView;
import team.startup.gwangsan.application.chat.data.ChatProductCard;
import team.startup.gwangsan.application.chat.data.ChatRoomListRow;
import team.startup.gwangsan.application.chat.data.ChatRoomMessages;
import team.startup.gwangsan.application.chat.data.ChatRoomProduct;
import team.startup.gwangsan.application.chat.data.ChatRoomSummary;
import team.startup.gwangsan.application.chat.data.LoadChatRoomMessagesCommand;
import team.startup.gwangsan.application.chat.data.UpdatedChatMessage;
import team.startup.gwangsan.application.chat.port.in.ChatRoomIdQuery;
import team.startup.gwangsan.application.chat.port.in.ChatRoomListQuery;
import team.startup.gwangsan.application.chat.port.in.CreateChatRoomUseCase;
import team.startup.gwangsan.application.chat.port.in.DeleteChatMessageUseCase;
import team.startup.gwangsan.application.chat.port.in.HideChatRoomUseCase;
import team.startup.gwangsan.application.chat.port.in.LoadChatRoomMessagesUseCase;
import team.startup.gwangsan.application.chat.port.in.ReadChatMessagesUseCase;
import team.startup.gwangsan.application.chat.port.in.UpdateChatMessageUseCase;
import team.startup.gwangsan.application.chat.port.in.ValidateChatSendableQuery;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("채팅 API 독립 HTTP 계약")
class ChatControllerStandaloneTest {

    static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);

    /** 인증 principal 대신 고정 실행자를 넣는다. 실제 resolver 는 ActorContextArgumentResolverTest 가 검증한다. */
    static final HandlerMethodArgumentResolver FIXED_ACTOR = new HandlerMethodArgumentResolver() {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return ActorContext.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return ACTOR;
        }
    };

    private MockMvc mvc;
    private CreateChatRoomUseCase createChatRoom;
    private LoadChatRoomMessagesUseCase loadMessages;
    private ReadChatMessagesUseCase readMessages;
    private ChatRoomListQuery roomList;
    private ChatRoomIdQuery roomId;
    private HideChatRoomUseCase hideRoom;
    private UpdateChatMessageUseCase updateMessage;
    private DeleteChatMessageUseCase deleteMessage;

    @BeforeEach
    void setUp() {
        createChatRoom = mock(CreateChatRoomUseCase.class);
        loadMessages = mock(LoadChatRoomMessagesUseCase.class);
        readMessages = mock(ReadChatMessagesUseCase.class);
        roomList = mock(ChatRoomListQuery.class);
        roomId = mock(ChatRoomIdQuery.class);
        hideRoom = mock(HideChatRoomUseCase.class);
        updateMessage = mock(UpdateChatMessageUseCase.class);
        deleteMessage = mock(DeleteChatMessageUseCase.class);
        ChatController controller = new ChatController(createChatRoom, loadMessages, readMessages, roomList, roomId,
                hideRoom, mock(ValidateChatSendableQuery.class), updateMessage, deleteMessage);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(FIXED_ACTOR)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ChatProductCard card(String mode, boolean author, boolean seller) {
        return new ChatProductCard(10L, "상품명", List.of(new ChatImage(1L, "p1")), null, seller, author, !seller,
                false, false, false, null, null, null, null, null, null, null);
    }

    @Nested
    @DisplayName("채팅방 생성은")
    class CreateRoom {
        @Test
        void it_binds_product_id_and_returns_created_json() throws Exception {
            when(createChatRoom.execute(ACTOR, 17L)).thenReturn(41L);

            mvc.perform(post("/api/chat/room/17"))
                    .andExpect(status().isCreated())
                    .andExpect(content().json("{\"roomId\":41}", true));

            verify(createChatRoom).execute(ACTOR, 17L);
        }
    }

    @Nested
    @DisplayName("채팅 메시지 조회는")
    class FindMessages {
        @Test
        void it_applies_default_pagination_when_cursor_is_omitted() throws Exception {
            when(loadMessages.execute(new LoadChatRoomMessagesCommand(ACTOR, 7L, null, null, 20)))
                    .thenReturn(new ChatRoomMessages(card("GIVER", true, true), List.of()));

            mvc.perform(get("/api/chat/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isEmpty());

            verify(loadMessages).execute(new LoadChatRoomMessagesCommand(ACTOR, 7L, null, null, 20));
        }

        @Test
        void it_binds_datetime_cursor_message_id_and_limit() throws Exception {
            LocalDateTime cursor = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
            when(loadMessages.execute(any())).thenReturn(new ChatRoomMessages(card("GIVER", true, true), List.of()));

            mvc.perform(get("/api/chat/7")
                            .param("lastCreatedAt", "2026-01-02T03:04:05")
                            .param("lastMessageId", "9")
                            .param("limit", "3"))
                    .andExpect(status().isOk());

            verify(loadMessages).execute(new LoadChatRoomMessagesCommand(ACTOR, 7L, cursor, 9L, 3));
        }

        @ParameterizedTest
        @CsvSource({"true, true", "true, false", "false, true", "false, false"})
        void it_serializes_product_and_message_with_existing_json_keys(boolean author, boolean seller) throws Exception {
            ChatMessageView message = new ChatMessageView(9L, 7L, "안녕", MessageType.IMAGE,
                    LocalDateTime.of(2026, 1, 2, 3, 4, 5), null, List.of(new ChatImage(3L, "u3")), "닉", 2L, true, false);
            when(loadMessages.execute(any())).thenReturn(new ChatRoomMessages(card("GIVER", author, seller), List.of(message)));

            mvc.perform(get("/api/chat/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.product.id").value(10))
                    .andExpect(jsonPath("$.product.images[0].imageId").value(1))
                    .andExpect(jsonPath("$.product.images[0].imageUrl").value("p1"))
                    .andExpect(jsonPath("$.product.isAuthor").isBoolean())
                    .andExpect(jsonPath("$.product.isAuthor").value(author))
                    .andExpect(jsonPath("$.product.author").doesNotExist())
                    .andExpect(jsonPath("$.product.isSeller").value(seller))
                    .andExpect(jsonPath("$.product.isCompletable").value(!seller))
                    .andExpect(jsonPath("$.product.isCompleted").value(false))
                    .andExpect(jsonPath("$.product.isReserved").value(false))
                    .andExpect(jsonPath("$.product.isDeleted").value(false))
                    .andExpect(jsonPath("$.product.createdAt").doesNotExist())
                    .andExpect(jsonPath("$.messages[0].messageId").value(9))
                    .andExpect(jsonPath("$.messages[0].messageType").value("IMAGE"))
                    .andExpect(jsonPath("$.messages[0].images[0].imageId").value(3))
                    .andExpect(jsonPath("$.messages[0].senderNickname").value("닉"))
                    .andExpect(jsonPath("$.messages[0].checked").value(true))
                    .andExpect(jsonPath("$.messages[0].isMine").value(false));
        }
    }

    @Nested
    @DisplayName("읽음 처리는")
    class ReadMessages {
        @Test
        void it_binds_message_ids_and_returns_ok_without_body() throws Exception {
            mvc.perform(patch("/api/chat/read")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"roomId\":12,\"lastMessageId\":33}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(readMessages).execute(ACTOR, 12L, 33L);
        }
    }

    @Nested
    @DisplayName("채팅방 목록과 삭제는")
    class Rooms {
        @Test
        void it_routes_rooms_before_room_id_and_serializes_room_metadata() throws Exception {
            when(roomList.query(ACTOR)).thenReturn(List.of(new ChatRoomSummary(
                    new ChatRoomListRow(2L, 3L, "상대", 9L, "안녕하세요", MessageType.TEXT, null, 1L, 10L),
                    new ChatRoomProduct(10L, null, false, false, true, List.of()))));

            mvc.perform(get("/api/chat/rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].roomId").value(2))
                    .andExpect(jsonPath("$[0].member.memberId").value(3))
                    .andExpect(jsonPath("$[0].member.nickname").value("상대"))
                    .andExpect(jsonPath("$[0].messageId").value(9))
                    .andExpect(jsonPath("$[0].lastMessageType").value("TEXT"))
                    .andExpect(jsonPath("$[0].unreadMessageCount").value(1))
                    .andExpect(jsonPath("$[0].product.productId").value(10))
                    .andExpect(jsonPath("$[0].product.isDeleted").value(true))
                    .andExpect(jsonPath("$[0].product.images").isEmpty());
        }

        @Test
        void it_binds_product_path_for_room_lookup() throws Exception {
            when(roomId.query(ACTOR, 11L)).thenReturn(2L);

            mvc.perform(get("/api/chat/room/11"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"roomId\":2}", true));
        }

        @Test
        void it_converts_missing_room_from_delete_to_not_found_response() throws Exception {
            doThrow(new NotFoundChatRoomException()).when(hideRoom).execute(ACTOR, 2L);

            mvc.perform(delete("/api/chat/room/2"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        void it_hides_room_with_no_content() throws Exception {
            mvc.perform(delete("/api/chat/room/2")).andExpect(status().isNoContent());

            verify(hideRoom).execute(ACTOR, 2L);
        }
    }

    @Nested
    @DisplayName("메시지 수정·삭제는")
    class Mutations {
        @Test
        void it_returns_updated_message_json() throws Exception {
            LocalDateTime editedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
            when(updateMessage.execute(ACTOR, 9L, "수정")).thenReturn(new UpdatedChatMessage(9L, 7L, "수정", editedAt));

            mvc.perform(patch("/api/chat/message/9").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messageId").value(9))
                    .andExpect(jsonPath("$.roomId").value(7))
                    .andExpect(jsonPath("$.content").value("수정"))
                    .andExpect(jsonPath("$.editedAt").exists());
        }

        @Test
        void it_rejects_blank_content_before_use_case() throws Exception {
            mvc.perform(patch("/api/chat/message/9").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\" \"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(updateMessage);
        }

        @Test
        void it_deletes_with_no_content_and_maps_business_errors() throws Exception {
            mvc.perform(delete("/api/chat/message/9")).andExpect(status().isNoContent());
            verify(deleteMessage).execute(ACTOR, 9L);

            doThrow(new GlobalException(ErrorCode.CHAT_MESSAGE_EXPIRED)).when(deleteMessage).execute(eq(ACTOR), eq(8L));
            mvc.perform(delete("/api/chat/message/8")).andExpect(status().isConflict());
        }
    }
}
