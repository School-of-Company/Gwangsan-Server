package team.startup.gwangsan.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_chat_message")
public class ChatMessage {

    @Id
    @Column(name = "message_id")
    private Long id;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "message_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "checked", nullable = false)
    private Boolean checked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @Builder
    public ChatMessage(Long id, String content, MessageType messageType, Boolean checked, ChatRoom room, Member sender, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.messageType = messageType;
        this.checked = checked;
        this.room = room;
        this.sender = sender;
        this.createdAt = createdAt;
    }

    public void updateChecked(Boolean checked) {
        this.checked = checked;
    }
}
