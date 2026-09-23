package team.startup.gwangsan.domain.chat.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.impl.ValidateChatSendableServiceImpl;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("채팅 sendable HTTP 계약")
class ChatSendableControllerTest {
    private MockMvc mvc;
    private ImageRepository imageRepository;

    @BeforeEach
    void setup() {
        ChatRoomRepository rooms = mock(ChatRoomRepository.class);
        MemberUtil members = mock(MemberUtil.class);
        BlockValidator blocks = mock(BlockValidator.class);
        imageRepository = mock(ImageRepository.class);
        Member member = mock(Member.class);
        ChatRoom room = mock(ChatRoom.class);
        when(members.getCurrentMember()).thenReturn(member);
        when(rooms.findChatRoomByRoomId(10L)).thenReturn(Optional.of(room));
        when(room.isParticipant(member)).thenReturn(true);
        ValidateChatSendableServiceImpl service = new ValidateChatSendableServiceImpl(rooms, members, blocks, imageRepository);
        ChatController controller = new ChatController(null, null, null, null, null, null, service);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Nested
    @DisplayName("sendable 요청은")
    class Describe_sendable {

        @Test
        @DisplayName("쿼리 생략 시 빈 본문을 유지한다")
        void it_absent_query_preserves_empty_success_body() throws Exception {
            mvc.perform(get("/api/chat/room/10/sendable"))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verifyNoInteractions(imageRepository);
        }

        @Test
        @DisplayName("반복 쿼리의 중복 메타데이터를 반환한다")
        void it_repeated_query_returns_ordered_image_metadata() throws Exception {
            Image image = mock(Image.class);
            when(image.getId()).thenReturn(11L);
            when(image.getImageUrl()).thenReturn("https://example.com/11.jpg");
            when(imageRepository.findAllById(List.of(11L, 11L))).thenReturn(List.of(image));
            mvc.perform(get("/api/chat/room/10/sendable?imageIds=11&imageIds=11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.images.length()").value(2))
                    .andExpect(jsonPath("$.images[0].imageId").value(11))
                    .andExpect(jsonPath("$.images[1].imageUrl").value("https://example.com/11.jpg"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "0", "-1", "1.5", "abc", "9007199254740992", "9223372036854775808", "+11", "11 ", " 11", "11,12", "0xB", "1e1", "１１"})
        @DisplayName("잘못된 형식은 조회 없이 400을 반환한다")
        void it_invalid_query_returns_bad_request_without_lookup(String id) throws Exception {
            mvc.perform(get("/api/chat/room/10/sendable").param("imageIds", id))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(imageRepository);
        }

        @Test
        @DisplayName("빈 값이 섞인 반복 쿼리는 400을 반환한다")
        void it_empty_repeated_value_returns_bad_request() throws Exception {
            mvc.perform(get("/api/chat/room/10/sendable").param("imageIds", "11", ""))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(imageRepository);
        }

        @Test
        @DisplayName("없는 이미지는 기존 404를 반환한다")
        void it_missing_image_returns_existing_not_found_status() throws Exception {
            when(imageRepository.findAllById(List.of(11L))).thenReturn(List.of());
            mvc.perform(get("/api/chat/room/10/sendable").param("imageIds", "11"))
                    .andExpect(status().isNotFound());
        }
        @Test
        @DisplayName("값 없는 쿼리를 생략으로 취급하지 않는다")
        void it_rejects_bare_query_key() throws Exception {
            mvc.perform(get("/api/chat/room/10/sendable?imageIds"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(imageRepository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1", "9007199254740991", "00011"})
        @DisplayName("유효한 단일 ID는 DB 메타데이터를 반환한다")
        void it_returns_single_image(String value) throws Exception {
            long id = Long.parseLong(value);
            Image image = mock(Image.class);
            when(image.getId()).thenReturn(id);
            when(image.getImageUrl()).thenReturn("https://example.com/image.jpg");
            when(imageRepository.findAllById(List.of(id))).thenReturn(List.of(image));
            mvc.perform(get("/api/chat/room/10/sendable?imageIds=" + value))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"images\":[{\"imageId\":" + id
                            + ",\"imageUrl\":\"https://example.com/image.jpg\"}]}"));
        }
    }

}
