package team.startup.gwangsan.application.dong.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.dong.data.DongResult;
import team.startup.gwangsan.application.dong.port.in.DongQuery;
import team.startup.gwangsan.application.dong.port.out.LoadDongPort;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DongQueryService implements DongQuery {

    private final LoadDongPort loadDongPort;

    @Override
    public Optional<DongResult> findByName(String name) {
        return loadDongPort.findByName(name);
    }
}
