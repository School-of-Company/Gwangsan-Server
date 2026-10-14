package team.startup.gwangsan.adapter.persistence.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.List;
import java.util.Optional;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomJpaEntity, Long>, ChatRoomCustomRepository {

    Optional<ChatRoomJpaEntity> findByProductIdAndBuyerAndSeller(Long productId, MemberJpaEntity buyer, MemberJpaEntity seller);

    List<ChatRoomJpaEntity> findAllByBuyerAndSeller(MemberJpaEntity buyer, MemberJpaEntity seller);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoomJpaEntity c SET c.buyer = :dummy WHERE c.buyer = :target")
    void reassignBuyer(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoomJpaEntity c SET c.seller = :dummy WHERE c.seller = :target")
    void reassignSeller(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
