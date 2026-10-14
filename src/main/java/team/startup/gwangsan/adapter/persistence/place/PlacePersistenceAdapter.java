package team.startup.gwangsan.adapter.persistence.place;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.out.LoadPlacePort;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * ponytail: JPA 모델은 아직 legacy(domain.place.entity/repository)다. member/post/trade/report/notice/admin 의
 * association·QueryDSL 이 Place/Head 를 직접 쓰므로 Entity 이동은 그 호출자 전환 뒤 수행한다.
 */
@Component
@RequiredArgsConstructor
public class PlacePersistenceAdapter implements LoadPlacePort {

    private final PlaceRepository placeRepository;

    @Override
    public Optional<PlaceResult> findById(Integer placeId) {
        return placeRepository.findById(placeId).map(PlacePersistenceAdapter::toResult);
    }

    @Override
    public List<PlaceResult> findAllByHeadId(Integer headId) {
        return placeRepository.findAllByHeadId(headId).stream().map(PlacePersistenceAdapter::toResult).toList();
    }

    @Override
    public List<PlaceResult> findAllByIds(Collection<Integer> placeIds) {
        return placeRepository.findAllById(placeIds).stream().map(PlacePersistenceAdapter::toResult).toList();
    }

    private static PlaceResult toResult(Place place) {
        // LAZY head 프록시의 id 접근은 본점 행을 추가 조회하지 않는다.
        return new PlaceResult(place.getId(), place.getName(), place.getHead() == null ? null : place.getHead().getId());
    }
}
