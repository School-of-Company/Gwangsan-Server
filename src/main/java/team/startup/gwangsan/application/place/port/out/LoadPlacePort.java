package team.startup.gwangsan.application.place.port.out;

import team.startup.gwangsan.application.place.data.PlaceResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadPlacePort {
    Optional<PlaceResult> findById(Integer placeId);

    List<PlaceResult> findAllByHeadId(Integer headId);

    List<PlaceResult> findAllByIds(Collection<Integer> placeIds);
}
