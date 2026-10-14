package team.startup.gwangsan.application.chat.port.out;

import team.startup.gwangsan.application.chat.data.ChatRoomListRow;

import java.util.List;

public interface ChatRoomListPort {
    /** 활성이고 본인이 숨기지 않은 방, 최신 메시지 시각 내림차순(메시지 없는 방은 뒤). */
    List<ChatRoomListRow> findVisibleRooms(Long memberId);
}
