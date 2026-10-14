package team.startup.gwangsan.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.custom.ChatRoomCustomRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomCustomRepository {
    Optional<ChatRoom> findByProductIdAndBuyerAndSeller(Long productId, MemberJpaEntity buyer, MemberJpaEntity seller);

    List<ChatRoom> findAllByBuyerAndSeller(MemberJpaEntity buyer, MemberJpaEntity seller);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.buyer = :dummy WHERE c.buyer = :target")
    void reassignBuyer(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.seller = :dummy WHERE c.seller = :target")
    void reassignSeller(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
