package team.startup.gwangsan.adapter.integration.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.SignUpLocationPort;
import team.startup.gwangsan.application.dong.data.DongResult;
import team.startup.gwangsan.application.dong.port.in.DongQuery;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SignUpLocationAdapter implements SignUpLocationPort {

    private final DongQuery dongQuery;
    private final PlaceQuery placeQuery;

    @Override
    public Optional<Integer> findDongIdByName(String dongName) {
        return dongQuery.findByName(dongName).map(DongResult::id);
    }

    @Override
    public boolean existsPlace(Integer placeId) {
        return placeQuery.findById(placeId).isPresent();
    }
}
