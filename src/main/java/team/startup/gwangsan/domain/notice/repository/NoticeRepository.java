package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 단일 장소
    List<Notice> findByPlaceOrderByIdDesc(Place place, Pageable pageable);
    List<Notice> findByPlaceAndIdLessThanOrderByIdDesc(Place place, Long lastId, Pageable pageable);

    // HEAD_ADMIN용 (다중 장소)
    List<Notice> findByPlaceInOrderByIdDesc(List<Place> places, Pageable pageable);
    List<Notice> findByPlaceInAndIdLessThanOrderByIdDesc(List<Place> places, Long lastId, Pageable pageable);
}
