package team.startup.gwangsan.adapter.persistence.chat;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.time.LocalDateTime;

/** tbl_chat_message 저장 모델. ID 는 채팅 서버가 만든 값(예약 취소 SYSTEM 메시지는 -예약ID)이다. */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_chat_message")
public class ChatMessageJpaEntity {

    @Id
    @Column(name = "message_id")
    private Long id;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "message_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "checked", nullable = false)
    private Boolean checked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomJpaEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private MemberJpaEntity sender;

    @Builder
    public ChatMessageJpaEntity(Long id, String content, MessageType messageType, Boolean checked,
                                ChatRoomJpaEntity room, MemberJpaEntity sender, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.messageType = messageType;
        this.checked = checked;
        this.room = room;
        this.sender = sender;
        this.createdAt = createdAt;
    }

    void applyMutation(String content, String originalContent, LocalDateTime editedAt, LocalDateTime deletedAt) {
        this.content = content;
        this.originalContent = originalContent;
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
    }
}
