package team.startup.gwangsan.application.notice.port.out;

import team.startup.gwangsan.application.notice.data.NoticePlace;

import java.util.List;
import java.util.Optional;

public interface NoticePlacePort {

    Optional<NoticePlace> findPlace(Integer placeId);

    // placeId 지점과 같은 본부에 속한 지점 id 전체(자기 포함).
    List<Integer> findPlaceIdsInSameHead(Integer placeId);
}
