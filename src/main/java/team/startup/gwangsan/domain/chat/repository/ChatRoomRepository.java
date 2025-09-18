package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.custom.ChatRoomCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomCustomRepository {
    Optional<ChatRoom> findByProductIdAndBuyerAndSeller(Long productId, Member buyer, Member seller);
}
