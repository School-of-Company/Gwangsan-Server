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

import static team.startup.gwangsan.domain.admin.entity.QAdminAlert.adminAlert;
import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;

@Repository
@RequiredArgsConstructor
public class AdminAlertCustomRepositoryImpl implements AdminAlertCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminAlert> findAdminAlertByPlaceAndAlertType(Place place, AlertType alertType) {
        return queryFactory
                .selectFrom(adminAlert).distinct()
                .join(adminAlert.member, member).fetchJoin()
                .join(memberDetail).on(member.id.eq(memberDetail.member.id)).fetchJoin()
                .where(
                        placeIdEq(place),
                        alertTypeEq(alertType)
                )
                .fetch();
    }

    private BooleanExpression placeIdEq(Place place) {
        return place != null ? memberDetail.place.id.eq(place.getId()) : null;
    }

    private BooleanExpression alertTypeEq(AlertType alertType) {
        return alertType != null ? adminAlert.type.eq(alertType) : null;
    }
}
