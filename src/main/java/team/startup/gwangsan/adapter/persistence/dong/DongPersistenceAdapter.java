package team.startup.gwangsan.adapter.persistence.dong;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.dong.data.DongResult;
import team.startup.gwangsan.application.dong.port.out.LoadDongPort;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DongPersistenceAdapter implements LoadDongPort {

    private final DongJpaRepository dongRepository;

    @Override
    public Optional<DongResult> findByName(String name) {
        return dongRepository.findByName(name).map(dong -> new DongResult(dong.getId(), dong.getName()));
    }
}
