package team.startup.gwangsan.domain.chat.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.CreateChatRoomResponse;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetChatMessagesResponse;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomIdResponse;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomMemberResponse;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomsResponse;
import team.startup.gwangsan.domain.chat.service.CreateChatRoomService;
import team.startup.gwangsan.domain.chat.service.DeleteChatRoomService;
import team.startup.gwangsan.domain.chat.service.FindChatMessageByRoomIdService;
import team.startup.gwangsan.domain.chat.service.FindRoomIdByProductIdService;
import team.startup.gwangsan.domain.chat.service.FindRoomsByCurrentUserService;
import team.startup.gwangsan.domain.chat.service.ReadChatMessageService;
import team.startup.gwangsan.domain.chat.service.ValidateChatSendableService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("채팅 API 독립 HTTP 계약")
class ChatControllerStandaloneTest {
    private MockMvc mvc;
    private CreateChatRoomService createChatRoomService;
    private FindChatMessageByRoomIdService findChatMessageByRoomIdService;
    private ReadChatMessageService readChatMessageService;
    private FindRoomsByCurrentUserService findRoomsByCurrentUserService;
    private FindRoomIdByProductIdService findRoomIdByProductIdService;
    private DeleteChatRoomService deleteChatRoomService;

    @BeforeEach
    void setUp() {
        createChatRoomService = mock(CreateChatRoomService.class);
        findChatMessageByRoomIdService = mock(FindChatMessageByRoomIdService.class);
        readChatMessageService = mock(ReadChatMessageService.class);
        findRoomsByCurrentUserService = mock(FindRoomsByCurrentUserService.class);
        findRoomIdByProductIdService = mock(FindRoomIdByProductIdService.class);
        deleteChatRoomService = mock(DeleteChatRoomService.class);
        ChatController controller = new ChatController(
                createChatRoomService,
                findChatMessageByRoomIdService,
                readChatMessageService,
                findRoomsByCurrentUserService,
                findRoomIdByProductIdService,
                deleteChatRoomService,
                mock(ValidateChatSendableService.class)
        );
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("채팅방 생성은")
    class CreateRoom {
        @Test
        void it_binds_product_id_and_returns_created_json() throws Exception {
            when(createChatRoomService.execute(17L)).thenReturn(new CreateChatRoomResponse(41L));

            mvc.perform(post("/api/chat/room/17"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roomId").value(41));

            verify(createChatRoomService).execute(17L);
        }
    }

    @Nested
    @DisplayName("채팅 메시지 조회는")
    class FindMessages {
        @Test
        void it_applies_default_pagination_when_cursor_is_omitted() throws Exception {
            when(findChatMessageByRoomIdService.execute(7L, null, null, 20))
                    .thenReturn(new GetChatMessagesResponse(null, java.util.List.of()));

            mvc.perform(get("/api/chat/7"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"product\":null,\"messages\":[]}"));

            verify(findChatMessageByRoomIdService).execute(7L, null, null, 20);
        }

        @Test
        void it_binds_datetime_cursor_message_id_and_limit() throws Exception {
            LocalDateTime cursor = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
            when(findChatMessageByRoomIdService.execute(7L, cursor, 9L, 3))
                    .thenReturn(new GetChatMessagesResponse(null, List.of()));

            mvc.perform(get("/api/chat/7")
                            .param("lastCreatedAt", "2026-01-02T03:04:05")
                            .param("lastMessageId", "9")
                            .param("limit", "3"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"product\":null,\"messages\":[]}"));

            verify(findChatMessageByRoomIdService).execute(7L, cursor, 9L, 3);
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

            verify(readChatMessageService).execute(12L, 33L);
        }
    }

    @Nested
    @DisplayName("채팅방 목록과 삭제는")
    class Rooms {
        @Test
        void it_routes_rooms_before_room_id_and_serializes_room_metadata() throws Exception {
            when(findRoomsByCurrentUserService.execute()).thenReturn(List.of(new GetRoomsResponse(
                    2L, new GetRoomMemberResponse(3L, "상대"), 9L, "안녕하세요", MessageType.TEXT,
                    null, 1L, null
            )));

            mvc.perform(get("/api/chat/rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].roomId").value(2))
                    .andExpect(jsonPath("$[0].member.nickname").value("상대"))
                    .andExpect(jsonPath("$[0].lastMessageType").value("TEXT"));

            verify(findRoomsByCurrentUserService).execute();
        }

        @Test
        void it_binds_product_path_for_room_lookup() throws Exception {
            when(findRoomIdByProductIdService.execute(11L)).thenReturn(new GetRoomIdResponse(2L));

            mvc.perform(get("/api/chat/room/11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomId").value(2));

            verify(findRoomIdByProductIdService).execute(11L);
        }

        @Test
        void it_converts_missing_room_from_delete_to_not_found_response() throws Exception {
            doThrow(new NotFoundChatRoomException()).when(deleteChatRoomService).execute(2L);

            mvc.perform(delete("/api/chat/room/2"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
