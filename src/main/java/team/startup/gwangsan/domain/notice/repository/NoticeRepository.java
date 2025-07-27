package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByPlaceInAndTargetRolesContainingOrderByIdDesc(List<Place> places, MemberRole role, Pageable pageable);

    List<Notice> findByPlaceInAndTargetRolesContainingAndIdLessThanOrderByIdDesc(List<Place> places, MemberRole role, Long lastId, Pageable pageable);

    List<Notice> findByPlaceAndTargetRolesContainingOrderByIdDesc(Place place, MemberRole role, Pageable pageable);

    List<Notice> findByPlaceAndTargetRolesContainingAndIdLessThanOrderByIdDesc(Place place, MemberRole role, Long lastId, Pageable pageable);

}
