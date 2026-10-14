package team.startup.gwangsan.application.chat.port.in;

import java.time.LocalDateTime;

/** 다른 업무(예약 취소 등)가 채팅방에 시스템 메시지를 남긴다. 호출자 트랜잭션에 참여한다. */
public interface SaveChatSystemMessageUseCase {

    /** 읽음 처리된 SYSTEM 메시지를 지정한 ID·시각으로 저장하고 메시지 ID 를 돌려준다. */
    Long save(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt);
}
