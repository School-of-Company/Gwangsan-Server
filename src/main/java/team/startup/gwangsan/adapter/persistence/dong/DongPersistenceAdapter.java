package team.startup.gwangsan.adapter.persistence.dong;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.dong.data.DongResult;
import team.startup.gwangsan.application.dong.port.out.LoadDongPort;
import team.startup.gwangsan.domain.dong.repository.DongRepository;

import java.util.Optional;

/** ponytail: JPA 모델은 아직 legacy(domain.dong). MemberDetail association(PR02)이 직접 쓰므로 그 전환 뒤 이동한다. */
@Component
@RequiredArgsConstructor
public class DongPersistenceAdapter implements LoadDongPort {

    private final DongRepository dongRepository;

    @Override
    public Optional<DongResult> findByName(String name) {
        return dongRepository.findByName(name).map(dong -> new DongResult(dong.getId(), dong.getName()));
    }
}
