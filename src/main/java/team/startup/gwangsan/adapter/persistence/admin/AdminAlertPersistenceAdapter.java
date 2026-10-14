package team.startup.gwangsan.adapter.persistence.admin;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.port.out.AdminAlertPort;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;

import java.util.List;
import java.util.Optional;

import static team.startup.gwangsan.adapter.persistence.admin.QAdminAlertJpaEntity.adminAlertJpaEntity;

@Component
@RequiredArgsConstructor
public class AdminAlertPersistenceAdapter implements AdminAlertPort {

    private static final QMemberJpaEntity member = QMemberJpaEntity.memberJpaEntity;
    private static final QMemberDetailJpaEntity memberDetail = QMemberDetailJpaEntity.memberDetailJpaEntity;

    private final AdminAlertJpaRepository adminAlertJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Optional<AdminAlert> findById(Long alertId) {
        return adminAlertJpaRepository.findById(alertId).map(AdminAlertPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<AdminAlert> findWithRequester(Long alertId) {
        return Optional.ofNullable(queryFactory
                        .selectFrom(adminAlertJpaEntity)
                        .join(adminAlertJpaEntity.requester, member).fetchJoin()
                        .where(adminAlertJpaEntity.id.eq(alertId))
                        .fetchOne())
                .map(AdminAlertPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<AdminAlert> findByIdAndType(Long alertId, AlertType type) {
        return adminAlertJpaRepository.findByIdAndType(alertId, type).map(AdminAlertPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<AdminAlert> findByTypeAndSourceId(AlertType type, Long sourceId) {
        return adminAlertJpaRepository.findByTypeAndSourceId(type, sourceId).map(AdminAlertPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsById(Long alertId) {
        return adminAlertJpaRepository.existsById(alertId);
    }

    @Override
    public void save(AlertType type, String title, Long sourceId, Long requesterId) {
        adminAlertJpaRepository.save(AdminAlertJpaEntity.builder()
                .type(type)
                .title(title)
                .sourceId(sourceId)
                .requester(entityManager.getReference(MemberJpaEntity.class, requesterId))
                .build());
    }

    @Override
    public void delete(Long alertId) {
        adminAlertJpaRepository.deleteById(alertId);
    }

    @Override
    public void reassignMember(Long memberId, Long dummyMemberId) {
        MemberJpaEntity target = entityManager.getReference(MemberJpaEntity.class, memberId);
        MemberJpaEntity dummy = entityManager.getReference(MemberJpaEntity.class, dummyMemberId);
        adminAlertJpaRepository.reassignOtherMember(target, dummy);
        adminAlertJpaRepository.reassignRequester(target, dummy);
    }

    /** 관리자 알림 목록 행: 알림과 fetch join 한 요청자 닉네임. */
    public record AlertRow(AdminAlert alert, String requesterNickname) {
    }

    /** 관리자 알림 목록. 요청자·회원 상세를 fetch join 해 지점으로 거른다(기존 쿼리 유지). */
    public List<AlertRow> findByPlacesAndType(List<Integer> placeIds, AlertType type) {
        return queryFactory
                .selectFrom(adminAlertJpaEntity).distinct()
                .join(adminAlertJpaEntity.requester, member).fetchJoin()
                .join(memberDetail).on(member.id.eq(memberDetail.member.id)).fetchJoin()
                .where(
                        memberDetail.place.id.in(placeIds),
                        typeEq(type)
                )
                .fetch()
                .stream()
                .map(entity -> new AlertRow(toDomain(entity), entity.getRequester().getNickname()))
                .toList();
    }

    private static BooleanExpression typeEq(AlertType type) {
        return type != null ? adminAlertJpaEntity.type.eq(type) : null;
    }

    private static AdminAlert toDomain(AdminAlertJpaEntity entity) {
        // 요청자 프록시의 ID 만 읽는다(지연 로딩 초기화 없음).
        Long requesterId = entity.getRequester() == null ? null : entity.getRequester().getId();
        return new AdminAlert(entity.getId(), entity.getType(), entity.getTitle(), entity.getSourceId(),
                requesterId, entity.getCreatedAt());
    }
}
