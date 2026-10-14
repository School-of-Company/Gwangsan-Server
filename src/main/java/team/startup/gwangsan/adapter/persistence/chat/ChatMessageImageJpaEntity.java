package team.startup.gwangsan.adapter.persistence.chat;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

/** tbl_chat_message_image 저장 모델. Image association 은 기존 FK 보존용 저장 기술 예외다. */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_chat_message_image")
public class ChatMessageImageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageJpaEntity image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessageJpaEntity chatMessage;

    @Builder
    public ChatMessageImageJpaEntity(ImageJpaEntity image, ChatMessageJpaEntity chatMessage) {
        this.image = image;
        this.chatMessage = chatMessage;
    }
}
