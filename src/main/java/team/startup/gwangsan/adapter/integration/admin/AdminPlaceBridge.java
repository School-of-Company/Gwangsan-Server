package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.port.out.AdminPlacePort;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;

import java.util.List;

/** place(PR03) 공개 query 연결. 목록 순서는 기존 findByHead 와 같다(PlaceQuery 계약). */
@Component
@RequiredArgsConstructor
public class AdminPlaceBridge implements AdminPlacePort {

    private final PlaceQuery placeQuery;

    @Override
    public List<Integer> placeIdsInHead(Integer headId) {
        return placeQuery.findAllByHeadId(headId).stream().map(PlaceResult::id).toList();
    }

    @Override
    public Integer requirePlace(Integer placeId) {
        return placeQuery.findById(placeId).orElseThrow(PlaceNotFoundException::new).id();
    }
}
