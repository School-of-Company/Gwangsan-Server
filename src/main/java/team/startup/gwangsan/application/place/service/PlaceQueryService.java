package team.startup.gwangsan.application.place.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.application.place.port.out.LoadPlacePort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryService implements PlaceQuery {

    private final LoadPlacePort loadPlacePort;

    @Override
    public Optional<PlaceResult> findById(Integer placeId) {
        return loadPlacePort.findById(placeId);
    }

    @Override
    public List<PlaceResult> findAllByHeadId(Integer headId) {
        return loadPlacePort.findAllByHeadId(headId);
    }

    @Override
    public List<PlaceResult> findAllByIds(Collection<Integer> placeIds) {
        return loadPlacePort.findAllByIds(placeIds);
    }
}
