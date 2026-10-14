package team.startup.gwangsan.application.dong.port.out;

import team.startup.gwangsan.application.dong.data.DongResult;

import java.util.Optional;

public interface LoadDongPort {
    Optional<DongResult> findByName(String name);
}
