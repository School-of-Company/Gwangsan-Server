package team.startup.gwangsan.domain.suspend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.suspend.entity.Suspend;

import java.time.LocalDateTime;
import java.util.List;

public interface SuspendRepository extends JpaRepository<Suspend, Long> {
    List<Suspend> findAllBySuspendedUntilBefore(LocalDateTime until);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Suspend s WHERE s.member = :member")
    void deleteAllByMember(@Param("member") Member member);
}
