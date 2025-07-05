package team.startup.gwangsan.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.image.entity.Image;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_chat_messege_image")
public class ChatMessageImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage chatMessage;

    @Builder
    public ChatMessageImage(Image image, ChatMessage chatMessage) {
        this.image = image;
        this.chatMessage = chatMessage;
    }
}
