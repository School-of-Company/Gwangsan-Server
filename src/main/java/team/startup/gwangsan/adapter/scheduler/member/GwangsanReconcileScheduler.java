package team.startup.gwangsan.adapter.scheduler.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.data.BalanceMismatch;
import team.startup.gwangsan.application.member.port.in.ReconcileGwangsanUseCase;

import java.util.List;

// ponytail: 읽기 전용이라 다중 인스턴스 중복 실행은 무해. 알림 연동은 Grafana 로그 알림으로 충분, 부족하면 관리자 알림 추가
@Slf4j
@Component
@RequiredArgsConstructor
public class GwangsanReconcileScheduler {

    private final ReconcileGwangsanUseCase reconcileGwangsanUseCase;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void reconcile() {
        try {
            List<BalanceMismatch> mismatches = reconcileGwangsanUseCase.execute();
            if (mismatches.isEmpty()) {
                log.info("[LEDGER-RECONCILE] 잔액과 원장 합이 모두 일치합니다");
                return;
            }
            for (BalanceMismatch m : mismatches) {
                log.error("[LEDGER-MISMATCH] memberId={} balance={} ledgerSum={}", m.memberId(), m.balance(), m.ledgerSum());
            }
        } catch (Exception e) {
            log.error("[LEDGER-RECONCILE] 대사 중 오류 발생", e);
        }
    }
}
