package team.startup.gwangsan.domain.admin.repository.custom.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.repository.custom.AdminAlertCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Optional;

import static team.startup.gwangsan.domain.admin.entity.QAdminAlert.adminAlert;
import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;

@Repository
@RequiredArgsConstructor
public class AdminAlertCustomRepositoryImpl implements AdminAlertCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminAlert> findAdminAlertByPlacesAndAlertType(List<Place> places, AlertType alertType) {
        return queryFactory
                .selectFrom(adminAlert).distinct()
                .join(adminAlert.requester, member).fetchJoin()
                .join(memberDetail).on(member.id.eq(memberDetail.member.id)).fetchJoin()
                .where(
                        memberDetail.place.in(places),
                        alertTypeEq(alertType)
                )
                .fetch();
    }

    @Override
    public Optional<AdminAlert> findByIdWithMember(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(adminAlert)
                        .join(adminAlert.requester, member).fetchJoin()
                        .where(adminAlert.id.eq(id))
                        .fetchOne()
        );
    }

    private BooleanExpression alertTypeEq(AlertType alertType) {
        return alertType != null ? adminAlert.type.eq(alertType) : null;
    }
}
