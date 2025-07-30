package team.startup.gwangsan.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;

import java.util.Optional;

public interface WithdrawalRecordRepository extends JpaRepository<WithdrawalRecord, Long> {
    Optional<WithdrawalRecord> findByPhoneNumber(String phoneNubmer);
}
