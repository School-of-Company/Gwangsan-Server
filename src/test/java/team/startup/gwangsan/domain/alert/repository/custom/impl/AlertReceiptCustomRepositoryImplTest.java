package team.startup.gwangsan.domain.alert.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("AlertReceiptCustomRepositoryImpl H2 슬라이스 통합 테스트")
class AlertReceiptCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    private AlertReceiptCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new AlertReceiptCustomRepositoryImpl(queryFactory);
    }

    @Nested
    @DisplayName("회원·읽음 상태·알림 ID 이하 영수증 조회는")
    class Describe_findByMemberIdAndCheckedUpToAlertId {

        @Test
        @DisplayName("현재 회원의 요청한 읽음 상태와 기준 알림 ID 이하 영수증만 반환한다")
        void it_limits_receipts_to_the_current_member_check_state_and_alert_id_cutoff() {
            Member currentMember = member("현재회원");
            Member otherMember = member("다른회원");
            Alert checkedAlert = alert(currentMember, "읽은 알림");
            Alert uncheckedAtCutoff = alert(currentMember, "기준 미확인 알림");
            Alert uncheckedAfterCutoff = alert(currentMember, "기준 이후 미확인 알림");
            AlertReceipt checkedReceipt = receipt(checkedAlert, currentMember, true);
            AlertReceipt uncheckedReceipt = receipt(uncheckedAtCutoff, currentMember, false);
            receipt(uncheckedAfterCutoff, currentMember, false);
            receipt(checkedAlert, otherMember, false);
            em.flush();
            em.clear();

            assertThat(repository.findByMemberIdAndCheckedUpToAlertId(
                    currentMember.getId(), false, uncheckedAtCutoff.getId()))
                    .extracting(AlertReceipt::getId)
                    .containsExactly(uncheckedReceipt.getId());
            assertThat(repository.findByMemberIdAndCheckedUpToAlertId(
                    currentMember.getId(), true, uncheckedAtCutoff.getId()))
                    .extracting(AlertReceipt::getId)
                    .containsExactly(checkedReceipt.getId());
        }
    }

    @Nested
    @DisplayName("회원 알림 조회는")
    class Describe_findByMemberId {

        @Test
        @DisplayName("현재 회원에게 도착한 읽은 알림과 미확인 알림을 생성 시각 내림차순으로 반환한다")
        void it_returns_only_current_member_alerts_in_descending_created_at_order() {
            Member currentMember = member("현재회원");
            Member sender = member("발신회원");
            Member otherMember = member("다른회원");
            Alert oldest = alert(sender, "오래된 알림");
            Alert newest = alert(sender, "최신 알림");
            Alert foreign = alert(sender, "제외 알림");
            receipt(oldest, currentMember, true);
            receipt(newest, currentMember, false);
            receipt(foreign, otherMember, false);
            em.flush();
            setCreatedAt(oldest, LocalDateTime.of(2024, 1, 1, 10, 0));
            setCreatedAt(newest, LocalDateTime.of(2024, 1, 2, 10, 0));
            setCreatedAt(foreign, LocalDateTime.of(2024, 1, 3, 10, 0));
            em.clear();

            List<Alert> result = repository.findByMemberId(currentMember.getId());

            assertThat(result).extracting(Alert::getId).containsExactly(newest.getId(), oldest.getId());
            assertThat(isLoaded(result.getFirst().getSendMember())).isTrue();
        }
    }

    private Member member(String suffix) {
        return em.persist(Member.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-2000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Alert alert(Member sender, String title) {
        return em.persist(Alert.builder()
                .sourceId(1L)
                .sendMember(sender)
                .alertType(AlertType.REVIEW)
                .title(title)
                .content("내용")
                .build());
    }

    private AlertReceipt receipt(Alert alert, Member member, boolean checked) {
        return em.persist(AlertReceipt.builder().alert(alert).member(member).checked(checked).build());
    }

    private void setCreatedAt(Alert alert, LocalDateTime createdAt) {
        em.getEntityManager().createNativeQuery("UPDATE tbl_alert SET created_at = ? WHERE alert_id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, alert.getId())
                .executeUpdate();
    }

    private boolean isLoaded(Object association) {
        return em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(association);
    }
}
