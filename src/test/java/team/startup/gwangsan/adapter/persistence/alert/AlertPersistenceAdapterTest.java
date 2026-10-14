package team.startup.gwangsan.adapter.persistence.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QueryDslConfig.class, AlertPersistenceAdapter.class})
@DisplayName("AlertPersistenceAdapter H2 슬라이스 테스트")
class AlertPersistenceAdapterTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private AlertPersistenceAdapter adapter;

    private MemberJpaEntity current;
    private MemberJpaEntity sender;
    private MemberJpaEntity other;

    @BeforeEach
    void setUp() {
        current = member("현재");
        sender = member("발신");
        other = member("다른");
    }

    @Nested
    @DisplayName("save() 는")
    class Describe_save {

        @Test
        @DisplayName("알림과 수신자별 미확인 receipt 를 기존 컬럼 그대로 저장한다")
        void it_saves_alert_and_receipts() {
            Long id = adapter.save(Alert.tradeCancel(77L, sender.getId()), List.of(current.getId(), other.getId()));
            em.flush();
            em.clear();

            AlertJpaEntity saved = em.find(AlertJpaEntity.class, id);
            assertThat(saved.getSourceId()).isEqualTo(77L);
            assertThat(saved.getSendMember().getId()).isEqualTo(sender.getId());
            assertThat(saved.getAlertType()).isEqualTo(AlertType.TRADE_CANCEL);
            assertThat(saved.getTitle()).isEqualTo("거래 철회 완료");
            assertThat(saved.getCreatedAt()).as("auditing").isNotNull();
            List<AlertReceiptJpaEntity> receipts = em.getEntityManager()
                    .createQuery("select r from AlertReceiptJpaEntity r where r.alert.id = :id order by r.id", AlertReceiptJpaEntity.class)
                    .setParameter("id", id).getResultList();
            assertThat(receipts).extracting(r -> r.getMember().getId()).containsExactly(current.getId(), other.getId());
            assertThat(receipts).allMatch(r -> !r.isChecked() && r.getCheckedAt() == null);
        }

        @Test
        @DisplayName("발신자가 없으면 send_member_id 를 null 로 둔다")
        void it_allows_null_sender() {
            Long id = adapter.save(Alert.notice(1L, "공지"), List.of(current.getId()));
            em.flush();
            em.clear();

            assertThat(em.find(AlertJpaEntity.class, id).getSendMember()).isNull();
        }

        @Test
        @DisplayName("같은 알림에 같은 수신자를 두 번 넣으면 unique 제약으로 실패한다")
        void it_keeps_unique_receipt_constraint() {
            assertThatThrownBy(() -> {
                adapter.save(Alert.notice(1L, "공지"), List.of(current.getId(), current.getId()));
                em.flush();
            }).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("loadReceived() 는")
    class Describe_loadReceived {

        @Test
        @DisplayName("현재 회원 수신 알림만 중복 없이 createdAt 내림차순으로 반환한다")
        void it_returns_received_alerts_desc() {
            Long oldest = adapter.save(Alert.review(1L, sender.getId(), "발신"), List.of(current.getId()));
            Long newest = adapter.save(Alert.review(2L, sender.getId(), "발신"), List.of(current.getId(), other.getId()));
            Long foreign = adapter.save(Alert.review(3L, sender.getId(), "발신"), List.of(other.getId()));
            em.flush();
            setCreatedAt(oldest, LocalDateTime.of(2024, 1, 1, 10, 0));
            setCreatedAt(newest, LocalDateTime.of(2024, 1, 2, 10, 0));
            setCreatedAt(foreign, LocalDateTime.of(2024, 1, 3, 10, 0));
            em.clear();

            List<Alert> result = adapter.loadReceived(current.getId());

            assertThat(result).extracting(Alert::id).containsExactly(newest, oldest);
            assertThat(result.getFirst().senderMemberId()).isEqualTo(sender.getId());
            assertThat(result.getFirst().createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 0));
        }
    }

    @Nested
    @DisplayName("markReadUpTo() 와 existsUnread() 는")
    class Describe_read {

        @Test
        @DisplayName("기준 알림 ID 이하의 현재 회원 미확인 receipt 만 읽음 처리한다")
        void it_marks_up_to_cutoff() {
            Long first = adapter.save(Alert.notice(1L, "a"), List.of(current.getId(), other.getId()));
            Long cutoff = adapter.save(Alert.notice(2L, "b"), List.of(current.getId()));
            Long after = adapter.save(Alert.notice(3L, "c"), List.of(current.getId()));
            em.flush();
            em.clear();
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 9, 0);

            adapter.markReadUpTo(current.getId(), cutoff, now);
            em.flush();
            em.clear();

            assertThat(receipt(first, current).isChecked()).isTrue();
            assertThat(receipt(first, current).getCheckedAt()).isEqualTo(now);
            assertThat(receipt(cutoff, current).isChecked()).isTrue();
            assertThat(receipt(after, current).isChecked()).isFalse();
            assertThat(receipt(first, other).isChecked()).isFalse();
            assertThat(adapter.existsUnread(current.getId())).isTrue();

            adapter.markReadUpTo(current.getId(), after, now.plusDays(1));
            em.flush();
            em.clear();
            assertThat(adapter.existsUnread(current.getId())).isFalse();
            assertThat(receipt(first, current).getCheckedAt()).as("이미 읽은 receipt 의 시각은 유지").isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("회원 정리는")
    class Describe_cleanup {

        @Test
        @DisplayName("발신자를 대체 회원으로 바꾸고 해당 회원 receipt 를 지운다")
        void it_reassigns_and_deletes() {
            MemberJpaEntity dummy = member("더미");
            Long id = adapter.save(Alert.review(1L, sender.getId(), "발신"), List.of(sender.getId(), current.getId()));
            em.flush();

            adapter.reassignSender(sender.getId(), dummy.getId());
            adapter.deleteReceipts(sender.getId());
            em.flush();
            em.clear();

            assertThat(em.find(AlertJpaEntity.class, id).getSendMember().getId()).isEqualTo(dummy.getId());
            assertThat(adapter.existsUnread(sender.getId())).isFalse();
            assertThat(adapter.existsUnread(current.getId())).isTrue();
        }
    }

    private AlertReceiptJpaEntity receipt(Long alertId, MemberJpaEntity member) {
        return em.getEntityManager()
                .createQuery("select r from AlertReceiptJpaEntity r where r.alert.id = :a and r.member.id = :m", AlertReceiptJpaEntity.class)
                .setParameter("a", alertId).setParameter("m", member.getId()).getSingleResult();
    }

    private MemberJpaEntity member(String suffix) {
        return em.persist(MemberJpaEntity.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-2000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private void setCreatedAt(Long alertId, LocalDateTime createdAt) {
        em.getEntityManager().createNativeQuery("UPDATE tbl_alert SET created_at = ? WHERE alert_id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, alertId)
                .executeUpdate();
    }
}
