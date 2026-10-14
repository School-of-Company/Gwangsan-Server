package team.startup.gwangsan.adapter.persistence.place;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.out.LoadPlacePort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PlacePersistenceAdapter implements LoadPlacePort {

    private final PlaceJpaRepository placeRepository;

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

    private static PlaceResult toResult(PlaceJpaEntity place) {
        // LAZY head 프록시의 id 접근은 본점 행을 추가 조회하지 않는다.
        return new PlaceResult(place.getId(), place.getName(), place.getHead() == null ? null : place.getHead().getId());
    }
}
