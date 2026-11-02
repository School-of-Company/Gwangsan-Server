package team.startup.gwangsan.adapter.integration.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.out.ValidatePlacePort;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;

/** 회원 본인 지점 변경·관리자 회원 목록 범위 검사에서 쓰는 지점 확인. place(PR) 공개 query 를 쓴다. */
@Component
@RequiredArgsConstructor
public class MemberPlaceBridge implements ValidatePlacePort {

    private final PlaceQuery placeQuery;

    @Override
    public void requireExists(Integer placeId) {
        placeQuery.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }

    @Override
    public boolean belongsToHead(Integer placeId, Integer headId) {
        return placeQuery.findById(placeId).map(place -> headId.equals(place.headId())).orElse(false);
    }
}
