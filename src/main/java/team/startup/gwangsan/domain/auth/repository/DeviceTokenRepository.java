package team.startup.gwangsan.domain.auth.repository;

import org.springframework.data.repository.CrudRepository;
import team.startup.gwangsan.domain.auth.entity.DeviceToken;

public interface DeviceTokenRepository extends CrudRepository<DeviceToken, String> {
}
