package team.startup.gwangsan.application.dong.port.in;

import team.startup.gwangsan.application.dong.data.DongResult;

import java.util.Optional;

public interface DongQuery {
    Optional<DongResult> findByName(String name);
}
