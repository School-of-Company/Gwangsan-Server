package team.startup.gwangsan.adapter.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WithdrawalRecordJpaRepository extends JpaRepository<WithdrawalRecordJpaEntity, Long> {
    Optional<WithdrawalRecordJpaEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndBannedIsTrue(String phoneNumber);
}
