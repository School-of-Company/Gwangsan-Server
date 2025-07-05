package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.member.entity.Member;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByMember1AndMember2(Member member1, Member member2);
}
