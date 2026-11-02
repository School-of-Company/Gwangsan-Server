package team.startup.gwangsan.adapter.persistence.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;

import java.time.LocalDateTime;

/**
 * 광산 원장 행. append-only 라 수정 메서드를 두지 않는다.
 * member_id 에 FK 를 걸지 않는다(탈퇴 후에도 이력 보존). 스키마는 V14__create_gwangsan_ledger.sql 과 같다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_gwangsan_ledger",
        uniqueConstraints = @UniqueConstraint(name = "uk_ledger_idem", columnNames = {"type", "ref_id", "member_id"}),
        indexes = @Index(name = "idx_ledger_member", columnList = "member_id,ledger_id"))
public class GwangsanLedgerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "amount", nullable = false, updatable = false)
    private Integer amount;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, updatable = false, length = 32)
    private GwangsanLedgerType type;

    /** ADMIN_ADJUST 는 null 이다. NULL 은 유니크 비교에서 서로 다르므로 조정은 몇 번이든 쌓인다. */
    @Column(name = "ref_id", updatable = false)
    private Long refId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static GwangsanLedgerJpaEntity of(long memberId, int amount, int balanceAfter, GwangsanLedgerType type, Long refId) {
        GwangsanLedgerJpaEntity entity = new GwangsanLedgerJpaEntity();
        entity.memberId = memberId;
        entity.amount = amount;
        entity.balanceAfter = balanceAfter;
        entity.type = type;
        entity.refId = refId;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}
