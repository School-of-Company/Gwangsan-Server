package team.startup.gwangsan.domain.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findAllByPlace(Place place, Pageable pageable);

    Page<Notice> findAllByPlaceIn(List<Place> places, Pageable pageable);
}