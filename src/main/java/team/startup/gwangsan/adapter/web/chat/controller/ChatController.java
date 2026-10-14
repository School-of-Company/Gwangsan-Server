package team.startup.gwangsan.adapter.web.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.chat.dto.ChatWebMapper;
import team.startup.gwangsan.adapter.web.chat.dto.CreateChatRoomResponse;
import team.startup.gwangsan.adapter.web.chat.dto.GetChatMessagesResponse;
import team.startup.gwangsan.adapter.web.chat.dto.GetRoomIdResponse;
import team.startup.gwangsan.adapter.web.chat.dto.GetRoomsResponse;
import team.startup.gwangsan.adapter.web.chat.dto.UpdateChatMessageRequest;
import team.startup.gwangsan.adapter.web.chat.dto.UpdateChatMessageResponse;
import team.startup.gwangsan.adapter.web.chat.dto.UpdateMessageCheckedRequest;
import team.startup.gwangsan.adapter.web.chat.dto.ValidateChatSendableResponse;
import team.startup.gwangsan.application.chat.data.LoadChatRoomMessagesCommand;
import team.startup.gwangsan.application.chat.port.in.ChatRoomIdQuery;
import team.startup.gwangsan.application.chat.port.in.ChatRoomListQuery;
import team.startup.gwangsan.application.chat.port.in.CreateChatRoomUseCase;
import team.startup.gwangsan.application.chat.port.in.DeleteChatMessageUseCase;
import team.startup.gwangsan.application.chat.port.in.HideChatRoomUseCase;
import team.startup.gwangsan.application.chat.port.in.LoadChatRoomMessagesUseCase;
import team.startup.gwangsan.application.chat.port.in.ReadChatMessagesUseCase;
import team.startup.gwangsan.application.chat.port.in.UpdateChatMessageUseCase;
import team.startup.gwangsan.application.chat.port.in.ValidateChatSendableQuery;
import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final LoadChatRoomMessagesUseCase loadChatRoomMessagesUseCase;
    private final ReadChatMessagesUseCase readChatMessagesUseCase;
    private final ChatRoomListQuery chatRoomListQuery;
    private final ChatRoomIdQuery chatRoomIdQuery;
    private final HideChatRoomUseCase hideChatRoomUseCase;
    private final ValidateChatSendableQuery validateChatSendableQuery;
    private final UpdateChatMessageUseCase updateChatMessageUseCase;
    private final DeleteChatMessageUseCase deleteChatMessageUseCase;

    @PatchMapping("/message/{message_id}")
    public ResponseEntity<UpdateChatMessageResponse> updateMessage(@PathVariable("message_id") Long messageId,
                                                                     @Valid @RequestBody UpdateChatMessageRequest request,
                                                                     ActorContext actor) {
        return ResponseEntity.ok(ChatWebMapper.toResponse(updateChatMessageUseCase.execute(actor, messageId, request.content())));
    }

    @DeleteMapping("/message/{message_id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable("message_id") Long messageId, ActorContext actor) {
        deleteChatMessageUseCase.execute(actor, messageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/room/{product_id}")
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(@PathVariable("product_id") Long productId, ActorContext actor) {
        Long roomId = createChatRoomUseCase.execute(actor, productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatRoomResponse(roomId));
    }

    @GetMapping("/{room_id}")
    public ResponseEntity<GetChatMessagesResponse> getChatMessage(
            @PathVariable("room_id") Long roomId,
            @RequestParam(name = "lastCreatedAt", required = false) LocalDateTime lastCreatedAt,
            @RequestParam(name = "lastMessageId", required = false) Long lastMessageId,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            ActorContext actor
    ) {
        return ResponseEntity.ok(ChatWebMapper.toResponse(loadChatRoomMessagesUseCase.execute(
                new LoadChatRoomMessagesCommand(actor, roomId, lastCreatedAt, lastMessageId, limit))));
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> readMessages(@RequestBody UpdateMessageCheckedRequest request, ActorContext actor) {
        readChatMessagesUseCase.execute(actor, request.roomId(), request.lastMessageId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<GetRoomsResponse>> getRooms(ActorContext actor) {
        return ResponseEntity.ok(ChatWebMapper.toRoomsResponse(chatRoomListQuery.query(actor)));
    }

    @GetMapping("/room/{product_id}")
    public ResponseEntity<GetRoomIdResponse> getRoom(@PathVariable("product_id") Long productId, ActorContext actor) {
        return ResponseEntity.ok(new GetRoomIdResponse(chatRoomIdQuery.query(actor, productId)));
    }

    // 채팅 서버가 소켓 메시지를 브로드캐스트하기 전에 호출한다. imageIds 가 없으면 200 + 빈 body.
    @GetMapping("/room/{room_id}/sendable")
    public ResponseEntity<ValidateChatSendableResponse> validateSendable(
            @PathVariable("room_id") Long roomId,
            @RequestParam MultiValueMap<String, String> queryParameters,
            ActorContext actor
    ) {
        return ResponseEntity.ok(validateChatSendableQuery.query(actor, roomId, queryParameters.get("imageIds"))
                .map(ChatWebMapper::toSendableResponse)
                .orElse(null));
    }

    @DeleteMapping("/room/{room_id}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable("room_id") Long roomId, ActorContext actor) {
        hideChatRoomUseCase.execute(actor, roomId);
        return ResponseEntity.noContent().build();
    }
}
