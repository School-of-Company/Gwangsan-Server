package team.startup.gwangsan.adapter.persistence.notification;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DeviceTokenRepository extends CrudRepository<DeviceToken, String> {
    List<DeviceToken> findAllByUserId(Long userId);
}
