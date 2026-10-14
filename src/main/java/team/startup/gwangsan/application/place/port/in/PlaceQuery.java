package team.startup.gwangsan.application.place.port.in;

import team.startup.gwangsan.application.place.data.PlaceResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 다른 업무가 쓰는 지점 공개 조회. 목록 순서는 기존 Repository 조회와 같게 별도 정렬하지 않는다. */
public interface PlaceQuery {
    Optional<PlaceResult> findById(Integer placeId);

    List<PlaceResult> findAllByHeadId(Integer headId);

    List<PlaceResult> findAllByIds(Collection<Integer> placeIds);
}
