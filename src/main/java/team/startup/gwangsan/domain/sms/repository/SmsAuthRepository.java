package team.startup.gwangsan.domain.sms.repository;

import org.springframework.data.repository.CrudRepository;
import team.startup.gwangsan.domain.sms.entity.SmsAuthEntity;

import java.util.Optional;

public interface SmsAuthRepository extends CrudRepository<SmsAuthEntity, String> {
    Optional<SmsAuthEntity> findByPhoneNumber(String phoneNumber);
}

