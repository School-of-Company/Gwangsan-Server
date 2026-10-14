package team.startup.gwangsan.adapter.integration.notice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.notice.data.NoticePlace;
import team.startup.gwangsan.application.notice.port.out.NoticePlacePort;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;

import java.util.List;
import java.util.Optional;

// place 공개 PlaceQuery(PR03)로 연결한다.
@Component
@RequiredArgsConstructor
public class NoticePlaceAdapter implements NoticePlacePort {

    private final PlaceQuery placeQuery;

    @Override
    public Optional<NoticePlace> findPlace(Integer placeId) {
        return placeQuery.findById(placeId)
                .map(place -> new NoticePlace(place.id(), place.name(), place.headId()));
    }

    // 본부가 없는 지점이면 본부가 없는 지점끼리 묶인다(기존 findByHead(null) 과 같음).
    @Override
    public List<Integer> findPlaceIdsInSameHead(Integer placeId) {
        Integer headId = placeQuery.findById(placeId).map(PlaceResult::headId).orElse(null);
        return placeQuery.findAllByHeadId(headId).stream()
                .map(PlaceResult::id)
                .toList();
    }
}
