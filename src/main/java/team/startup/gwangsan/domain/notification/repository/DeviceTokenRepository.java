package team.startup.gwangsan.domain.notification.repository;

import org.springframework.data.repository.CrudRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;

public interface DeviceTokenRepository extends CrudRepository<DeviceToken, String> {
}
