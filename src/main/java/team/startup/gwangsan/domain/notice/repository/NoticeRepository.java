package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByPlaceInOrderByIdDesc(List<Place> places, Pageable pageable);

    List<Notice> findByPlaceInAndIdLessThanOrderByIdDesc(List<Place> places, Long lastId, Pageable pageable);

    List<Notice> findByPlaceOrderByIdDesc(Place place, Pageable pageable);

    List<Notice> findByPlaceAndIdLessThanOrderByIdDesc(Place place, Long lastId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notice n SET n.member = :dummy WHERE n.member = :target")
    void reassignMember(@Param("target") Member target, @Param("dummy") Member dummy);
}
