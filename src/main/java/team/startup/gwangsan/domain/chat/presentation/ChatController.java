package team.startup.gwangsan.domain.chat.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.chat.presentation.dto.request.SaveChatMessageRequest;
import team.startup.gwangsan.domain.chat.presentation.dto.request.UpdateMessageCheckedRequest;
import team.startup.gwangsan.domain.chat.presentation.dto.response.*;
import team.startup.gwangsan.domain.chat.service.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final SaveChatMessageService saveChatMessageService;
    private final CreateChatRoomService createChatRoomService;
    private final FindChatMessageByRoomIdService findChatMessageByRoomIdService;
    private final ReadChatMessageService readChatMessageService;
    private final FindRoomsByCurrentUserService findRoomsByCurrentUserService;
    private final FindRoomIdByProductIdService findRoomIdByProductIdService;

    @PostMapping("/room/{product_id}")
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(@PathVariable("product_id") Long productId) {
        CreateChatRoomResponse response = createChatRoomService.execute(productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/message")
    public ResponseEntity<SaveChatMessageResponse> saveChatMessage(@RequestBody SaveChatMessageRequest request) {
        SaveChatMessageResponse response = saveChatMessageService.execute(request.roomId(), request.content(), request.imageIds(), request.messageType());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{room_id}")
    public ResponseEntity<GetChatMessagesResponse> getChatMessage(
            @PathVariable("room_id") Long roomId,
            @RequestParam(name = "lastCreatedAt", required = false) LocalDateTime lastCreatedAt,
            @RequestParam(name = "lastMessageId", required = false) Long lastMessageId,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        GetChatMessagesResponse responses = findChatMessageByRoomIdService.execute(roomId, lastCreatedAt, lastMessageId, limit);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> readMessages(@RequestBody UpdateMessageCheckedRequest request) {
        readChatMessageService.execute(request.roomId(), request.lastMessageId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<GetRoomsResponse>> getRooms() {
        List<GetRoomsResponse> responses = findRoomsByCurrentUserService.execute();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/room/{product_id}")
    public ResponseEntity<GetRoomIdResponse> getRoom(@PathVariable("product_id") Long productId) {
        GetRoomIdResponse response = findRoomIdByProductIdService.execute(productId);
        return ResponseEntity.ok(response);
    }
}
