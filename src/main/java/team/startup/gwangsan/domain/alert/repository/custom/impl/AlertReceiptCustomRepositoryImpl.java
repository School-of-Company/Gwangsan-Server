package team.startup.gwangsan.domain.alert.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.repository.custom.AlertReceiptCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.alert.entity.QAlert.alert;
import static team.startup.gwangsan.domain.alert.entity.QAlertReceipt.alertReceipt;

@Repository
@RequiredArgsConstructor
public class AlertReceiptCustomRepositoryImpl implements AlertReceiptCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AlertReceipt> findByMemberIdAndCheckedAndAlertId(Long memberId, boolean check, Long alertId) {
        return queryFactory
                .selectFrom(alertReceipt)
                .where(
                        alertReceipt.member.id.eq(memberId),
                        alertReceipt.alert.id.loe(alertId),
                        alertReceipt.checked.eq(check)
                )
                .fetch();
    }

    @Override
    public List<Alert> findByMemberId(Long memberId) {
        return queryFactory
                .select(alert).distinct()
                .from(alertReceipt)
                .join(alertReceipt.alert, alert)
                .leftJoin(alert.sendMember).fetchJoin()
                .where(alertReceipt.member.id.eq(memberId))
                .orderBy(alert.createdAt.desc())
                .fetch();
    }
}
