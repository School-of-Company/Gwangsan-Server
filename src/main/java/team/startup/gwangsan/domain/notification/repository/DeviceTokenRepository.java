package team.startup.gwangsan.domain.notification.repository;

import org.springframework.data.repository.CrudRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;

import java.util.Optional;

public interface DeviceTokenRepository extends CrudRepository<DeviceToken, String> {
    Optional<DeviceToken> findByUserId(Long userId);
}
