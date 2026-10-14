package team.startup.gwangsan.adapter.persistence.alert;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.alert.port.out.AlertMemberCleanupPort;
import team.startup.gwangsan.application.alert.port.out.LoadAlertPort;
import team.startup.gwangsan.application.alert.port.out.MarkAlertsReadPort;
import team.startup.gwangsan.application.alert.port.out.SaveAlertPort;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.time.LocalDateTime;
import java.util.List;

import static team.startup.gwangsan.adapter.persistence.alert.QAlertJpaEntity.alertJpaEntity;
import static team.startup.gwangsan.adapter.persistence.alert.QAlertReceiptJpaEntity.alertReceiptJpaEntity;

/**
 * 트랜잭션은 호출한 application service 가 연다. 이 Adapter 는 새 트랜잭션을 열지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AlertPersistenceAdapter implements SaveAlertPort, LoadAlertPort, MarkAlertsReadPort, AlertMemberCleanupPort {

    private final AlertJpaRepository alertJpaRepository;
    private final AlertReceiptJpaRepository alertReceiptJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Long save(Alert alert, List<Long> recipientMemberIds) {
        AlertJpaEntity saved = alertJpaRepository.save(AlertJpaEntity.builder()
                .sourceId(alert.sourceId())
                .sendMember(alert.senderMemberId() == null ? null : memberRef(alert.senderMemberId()))
                .alertType(alert.type())
                .title(alert.title())
                .content(alert.content())
                .build());
        alertReceiptJpaRepository.saveAll(recipientMemberIds.stream()
                .map(memberId -> AlertReceiptJpaEntity.builder()
                        .alert(saved)
                        .member(memberRef(memberId))
                        .checked(false)
                        .build())
                .toList());
        return saved.getId();
    }

    @Override
    public List<Alert> loadReceived(Long memberId) {
        return queryFactory
                .select(alertJpaEntity).distinct()
                .from(alertReceiptJpaEntity)
                .join(alertReceiptJpaEntity.alert, alertJpaEntity)
                .leftJoin(alertJpaEntity.sendMember).fetchJoin()
                .where(alertReceiptJpaEntity.member.id.eq(memberId))
                .orderBy(alertJpaEntity.createdAt.desc())
                .fetch()
                .stream()
                .map(AlertPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean existsUnread(Long memberId) {
        return alertReceiptJpaRepository.existsByMemberIdAndChecked(memberId, false);
    }

    @Override
    public void markReadUpTo(Long memberId, Long alertId, LocalDateTime checkedAt) {
        // 관리 상태 Entity 를 변경하고 호출 트랜잭션 커밋 시 반영한다 (기존과 같은 행 단위 UPDATE).
        queryFactory
                .selectFrom(alertReceiptJpaEntity)
                .where(
                        alertReceiptJpaEntity.member.id.eq(memberId),
                        alertReceiptJpaEntity.alert.id.loe(alertId),
                        alertReceiptJpaEntity.checked.eq(false)
                )
                .fetch()
                .forEach(receipt -> receipt.markChecked(checkedAt));
    }

    @Override
    public void reassignSender(Long memberId, Long dummyMemberId) {
        alertJpaRepository.reassignSendMember(memberRef(memberId), memberRef(dummyMemberId));
    }

    @Override
    public void deleteReceipts(Long memberId) {
        alertReceiptJpaRepository.deleteAllByMember(memberRef(memberId));
    }

    private MemberJpaEntity memberRef(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }

    private static Alert toDomain(AlertJpaEntity e) {
        return new Alert(
                e.getId(),
                e.getSourceId(),
                e.getSendMember() == null ? null : e.getSendMember().getId(),
                e.getAlertType(),
                e.getTitle(),
                e.getContent(),
                e.getCreatedAt()
        );
    }
}
