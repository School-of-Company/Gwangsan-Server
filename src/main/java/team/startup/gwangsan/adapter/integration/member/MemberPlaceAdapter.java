package team.startup.gwangsan.adapter.integration.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.out.MemberPlacePort;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;

@Component
@RequiredArgsConstructor
public class MemberPlaceAdapter implements MemberPlacePort {
    private final PlaceQuery placeQuery;

    @Override
    public void validate(Integer placeId) {
        placeQuery.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }
}
