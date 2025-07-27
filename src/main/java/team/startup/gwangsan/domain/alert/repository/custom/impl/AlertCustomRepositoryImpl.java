package team.startup.gwangsan.domain.alert.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.repository.custom.AlertCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.alert.entity.QAlert.alert;

@Repository
@RequiredArgsConstructor
public class AlertCustomRepositoryImpl implements AlertCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Alert> findUnreadAlerts(Long alertId, Long memberId) {
        return queryFactory
                .selectFrom(alert)
                .where(
                        alert.checked.isFalse(),
                        alert.id.loe(alertId),
                        alert.member.id.eq(memberId)
                )
                .fetch();
    }
}
