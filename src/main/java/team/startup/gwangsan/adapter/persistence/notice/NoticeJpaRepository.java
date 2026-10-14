package team.startup.gwangsan.adapter.persistence.notice;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.List;

public interface NoticeJpaRepository extends JpaRepository<NoticeJpaEntity, Long> {
    List<NoticeJpaEntity> findByPlaceIdInOrderByIdDesc(List<Integer> placeIds, Pageable pageable);

    List<NoticeJpaEntity> findByPlaceIdInAndIdLessThanOrderByIdDesc(List<Integer> placeIds, Long lastId, Pageable pageable);

    // 회원 탈퇴 재지정. 공개 command ReassignNoticeAuthorUseCase 가 NoticePort 를 통해 사용한다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NoticeJpaEntity n SET n.member = :dummy WHERE n.member = :target")
    void reassignMember(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
