package team.startup.gwangsan.application.auth.port.out;

import java.util.Optional;

/** 가입 시 동 이름·지점 확인. dong/place 업무 소유 데이터다. */
public interface SignUpLocationPort {

    Optional<Integer> findDongIdByName(String dongName);

    boolean existsPlace(Integer placeId);
}
