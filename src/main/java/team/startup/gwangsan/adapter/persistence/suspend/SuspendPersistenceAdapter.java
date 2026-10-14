package team.startup.gwangsan.adapter.persistence.suspend;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.suspend.port.out.LoadExpiredSuspensionPort;
import team.startup.gwangsan.application.suspend.port.out.LoadSuspensionPort;
import team.startup.gwangsan.application.suspend.port.out.SaveSuspensionPort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.suspend.model.Suspension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SuspendPersistenceAdapter implements SaveSuspensionPort, LoadExpiredSuspensionPort, LoadSuspensionPort {

    private final SuspendJpaRepository suspendJpaRepository;
    private final EntityManager entityManager;

    // 회원 association 은 기존 FK 매핑 유지를 위한 저장 기술 예외다. 존재 확인을 마친 ID 의 reference 로 채운다.
    @Override
    public Long save(Suspension suspension) {
        return suspendJpaRepository.save(SuspendJpaEntity.builder()
                        .member(entityManager.getReference(MemberJpaEntity.class, suspension.memberId()))
                        .suspendedAt(suspension.suspendedAt())
                        .suspendedDays(suspension.suspendedDays())
                        .suspendedUntil(suspension.suspendedUntil())
                        .build())
                .getId();
    }

    @Override
    public List<Long> findReleasableMemberIds(LocalDateTime now) {
        return suspendJpaRepository.findReleasableMemberIds(now);
    }

    @Override
    public Optional<Integer> findSuspendedDays(Long suspendId) {
        return suspendJpaRepository.findById(suspendId).map(SuspendJpaEntity::getSuspendedDays);
    }
}
