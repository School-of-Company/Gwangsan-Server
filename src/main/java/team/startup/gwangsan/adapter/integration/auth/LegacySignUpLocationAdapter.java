package team.startup.gwangsan.adapter.integration.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.SignUpLocationPort;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.dong.repository.DongRepository;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;

import java.util.Optional;

// bridge: dong/place 공개 query 가 생기면(PR03) 그 input Port 호출로 교체한다.
@Component
@RequiredArgsConstructor
public class LegacySignUpLocationAdapter implements SignUpLocationPort {

    private final DongRepository dongRepository;
    private final PlaceRepository placeRepository;

    @Override
    public Optional<Integer> findDongIdByName(String dongName) {
        return dongRepository.findByName(dongName).map(Dong::getId);
    }

    @Override
    public boolean existsPlace(Integer placeId) {
        return placeRepository.findById(placeId).isPresent();
    }
}
