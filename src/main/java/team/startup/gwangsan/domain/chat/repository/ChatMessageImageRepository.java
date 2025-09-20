package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageImageCustomRepository;

public interface ChatMessageImageRepository extends JpaRepository<ChatMessageImage, Long>, ChatMessageImageCustomRepository {
}
