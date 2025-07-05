package team.startup.gwangsan.domain.chat.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.chat.presentation.dto.request.CreateChatRoomRequest;
import team.startup.gwangsan.domain.chat.presentation.dto.response.CreateChatRoomResponse;
import team.startup.gwangsan.domain.chat.service.CreateChatRoomService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final CreateChatRoomService createChatRoomService;

    @PostMapping("/room")
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(@RequestBody CreateChatRoomRequest request) {
        CreateChatRoomResponse response = createChatRoomService.execute(request.roomId());
        return ResponseEntity.ok(response);
    }
}
