package team.startup.gwangsan.adapter.persistence.alert;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import team.startup.gwangsan.application.alert.data.RecordAlertCommand;
import team.startup.gwangsan.application.alert.port.in.RecordAlertUseCase;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort;
import team.startup.gwangsan.application.alert.service.CreateAlertService;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 중요 알림 저장 Port 가 호출한 업무 트랜잭션에 참여하는지 실제 Spring 프록시와 DB(H2) 로 확인한다.
 * MariaDB 동일 검증은 Docker suite 에서 수행한다 (evidence 대기 명령 참고).
 */
@DataJpaTest
@Import({QueryDslConfig.class, AlertPersistenceAdapter.class, CreateAlertService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("중요 알림 저장의 트랜잭션 경계")
class AlertRecordTransactionTest {

    @MockitoBean
    private LoadAlertSourcePort loadAlertSourcePort;
    @Autowired
    private RecordAlertUseCase recordAlertUseCase;
    @Autowired
    private EntityManager em;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            em.createQuery("delete from AlertReceiptJpaEntity").executeUpdate();
            em.createQuery("delete from AlertJpaEntity").executeUpdate();
            em.createQuery("delete from Member").executeUpdate();
        });
    }

    @Test
    @DisplayName("업무 트랜잭션이 알림 저장 뒤 실패하면 업무 변경·알림·receipt 가 모두 rollback 된다")
    void it_rolls_back_business_alert_and_receipts_together() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            MemberJpaEntity buyer = persistMember("buyer");
            MemberJpaEntity seller = persistMember("seller");
            recordAlertUseCase.record(new RecordAlertCommand(Alert.tradeCancel(1L, buyer.getId()),
                    List.of(buyer.getId(), seller.getId())));
            em.flush();
            throw new IllegalStateException("업무 후속 단계 실패");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(count("Member")).isZero();
        assertThat(count("AlertJpaEntity")).isZero();
        assertThat(count("AlertReceiptJpaEntity")).isZero();
    }

    @Test
    @DisplayName("알림 저장이 실패하면 같은 트랜잭션의 업무 변경도 rollback 된다")
    void it_rolls_back_business_when_alert_save_fails() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            MemberJpaEntity buyer = persistMember("buyer");
            // 같은 수신자 중복으로 receipt unique 제약 위반을 일으킨다.
            recordAlertUseCase.record(new RecordAlertCommand(Alert.tradeCancel(1L, buyer.getId()),
                    List.of(buyer.getId(), buyer.getId())));
            em.flush();
        })).isInstanceOf(RuntimeException.class);

        assertThat(count("Member")).isZero();
        assertThat(count("AlertJpaEntity")).isZero();
    }

    @Test
    @DisplayName("업무가 커밋되면 알림 1건과 수신자 receipt 가 함께 커밋된다")
    void it_commits_once_with_business() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            MemberJpaEntity buyer = persistMember("buyer");
            MemberJpaEntity seller = persistMember("seller");
            recordAlertUseCase.record(new RecordAlertCommand(Alert.tradeCancel(1L, buyer.getId()),
                    List.of(buyer.getId(), seller.getId())));
        });

        assertThat(count("Member")).isEqualTo(2);
        assertThat(count("AlertJpaEntity")).isEqualTo(1);
        assertThat(count("AlertReceiptJpaEntity")).isEqualTo(2);
    }

    private MemberJpaEntity persistMember(String suffix) {
        MemberJpaEntity member = MemberJpaEntity.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-3000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build();
        em.persist(member);
        return member;
    }

    private long count(String entity) {
        return new TransactionTemplate(transactionManager).execute(s ->
                em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult());
    }
}
