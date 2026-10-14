package team.startup.gwangsan.adapter.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import team.startup.gwangsan.adapter.event.report.ReportAlertEventAdapter;
import team.startup.gwangsan.adapter.event.suspend.SuspensionAlertEventAdapter;
import team.startup.gwangsan.adapter.integration.block.BlockTargetBridgeAdapter;
import team.startup.gwangsan.adapter.integration.report.ReportImageBridgeAdapter;
import team.startup.gwangsan.adapter.integration.report.ReportTargetBridgeAdapter;
import team.startup.gwangsan.adapter.integration.suspend.AdminAlertBridgeAdapter;
import team.startup.gwangsan.adapter.integration.suspend.MemberStatusBridgeAdapter;
import team.startup.gwangsan.adapter.persistence.block.MemberBlockJpaEntity;
import team.startup.gwangsan.adapter.persistence.block.MemberBlockPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.block.custom.impl.MemberBlockCustomRepositoryImpl;
import team.startup.gwangsan.adapter.persistence.report.ReportImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.ReportPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.suspend.SuspendJpaEntity;
import team.startup.gwangsan.adapter.persistence.suspend.SuspendJpaRepository;
import team.startup.gwangsan.adapter.persistence.suspend.SuspendPersistenceAdapter;
import team.startup.gwangsan.application.block.data.BlockedMember;
import team.startup.gwangsan.application.block.data.MemberBlockCommand;
import team.startup.gwangsan.application.block.service.BlockMemberService;
import team.startup.gwangsan.application.block.service.BlockQueryService;
import team.startup.gwangsan.application.block.service.UnblockMemberService;
import team.startup.gwangsan.application.report.data.CreateReportCommand;
import team.startup.gwangsan.application.report.service.CreateReportService;
import team.startup.gwangsan.application.suspend.data.SuspendMemberCommand;
import team.startup.gwangsan.application.suspend.service.ReleaseExpiredSuspensionsService;
import team.startup.gwangsan.application.suspend.service.SuspendMemberService;
import team.startup.gwangsan.domain.block.exception.AlreadyBlockedException;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.block.exception.NotFoundBlockException;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.model.ReportTargetType;
import team.startup.gwangsan.domain.report.model.ReportType;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertEvent;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;
import team.startup.gwangsan.adapter.persistence.image.ImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.member.MemberPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.post.ProductImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.post.ProductPersistenceAdapter;
import team.startup.gwangsan.application.image.service.ImageQueryService;
import team.startup.gwangsan.application.member.service.MemberAccountService;
import team.startup.gwangsan.application.post.service.ProductPublicQueryService;
import team.startup.gwangsan.application.report.data.ReportAlertSource;
import team.startup.gwangsan.application.report.data.ReportImageLink;
import team.startup.gwangsan.application.report.service.ReportQueryService;
import team.startup.gwangsan.application.suspend.service.SuspensionQueryService;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.suspend.exception.NotFoundSuspendException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * 신고·차단·정지 유스케이스를 실제 JPA Adapter·legacy bridge 와 H2 로 실행한다.
 * 이벤트는 ApplicationEvents 대신 기록용 listener 로 확인한다(AFTER_COMMIT listener 는 슬라이스에 없음).
 */
@DataJpaTest
@Import({QueryDslConfig.class,
        CreateReportService.class, ReportPersistenceAdapter.class, ReportTargetBridgeAdapter.class,
        ReportImageBridgeAdapter.class, ReportAlertEventAdapter.class,
        BlockMemberService.class, UnblockMemberService.class, BlockQueryService.class,
        MemberBlockPersistenceAdapter.class, MemberBlockCustomRepositoryImpl.class, BlockTargetBridgeAdapter.class,
        SuspendMemberService.class, ReleaseExpiredSuspensionsService.class, SuspendPersistenceAdapter.class,
        MemberStatusBridgeAdapter.class, AdminAlertBridgeAdapter.class, SuspensionAlertEventAdapter.class,
        team.startup.gwangsan.application.admin.service.AdminAlertSupportService.class,
        team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter.class,
        ReportQueryService.class, SuspensionQueryService.class,
        // 선행 업무 공개 Port 실제 구현(bridge 연결 대상): member·post·image
        MemberAccountService.class, MemberPersistenceAdapter.class,
        ProductPublicQueryService.class, ProductPersistenceAdapter.class, ProductImagePersistenceAdapter.class,
        ImageQueryService.class, ImagePersistenceAdapter.class,
        ReportBlockSuspendBoundaryIntegrationTest.RecordedEvents.class})
@DisplayName("신고·차단·정지 경계 통합 테스트 (H2)")
class ReportBlockSuspendBoundaryIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private CreateReportService createReportService;
    @Autowired private BlockMemberService blockMemberService;
    @Autowired private UnblockMemberService unblockMemberService;
    @Autowired private BlockQueryService blockQueryService;
    @Autowired private SuspendMemberService suspendMemberService;
    @Autowired private ReleaseExpiredSuspensionsService releaseExpiredSuspensionsService;
    @Autowired private RecordedEvents events;
    @Autowired private ReportQueryService reportQueryService;
    @Autowired private SuspensionQueryService suspensionQueryService;

    @org.junit.jupiter.api.BeforeEach
    void clearEvents() {
        events.published.clear();
    }

    static class RecordedEvents {
        final List<Object> published = new java.util.ArrayList<>();

        @org.springframework.context.event.EventListener
        void on(CreateAdminAlertEvent event) {
            published.add(event);
        }

        @org.springframework.context.event.EventListener
        void on(CreateAlertEvent event) {
            published.add(event);
        }
    }

    @Nested
    @DisplayName("신고는")
    class Describe_report {

        @Test
        @DisplayName("it_회원 신고를 저장하고 존재하는 이미지만 연결한 뒤 관리자 알림 이벤트를 발행한다")
        void it_saves_member_report_with_existing_images() {
            MemberJpaEntity reporter = member("신고자");
            MemberJpaEntity reported = member("피신고자");
            Image image = em.persist(Image.builder().imageUrl("https://img/1").build());

            createReportService.execute(new CreateReportCommand(reporter.getId(), ReportTargetType.MEMBER, reported.getId(),
                    ReportType.SPAM_AD, "스팸", List.of(image.getId(), 999_999L)));
            em.flush();
            em.clear();

            ReportJpaEntity saved = singleReport();
            assertThat(saved.getReporter().getId()).isEqualTo(reporter.getId());
            assertThat(saved.getReported().getId()).isEqualTo(reported.getId());
            assertThat(saved.getProduct()).isNull();
            assertThat(saved.getReportType()).isEqualTo(ReportType.SPAM_AD);
            List<ReportImageJpaEntity> images = em.getEntityManager()
                    .createQuery("select ri from ReportImageJpaEntity ri", ReportImageJpaEntity.class).getResultList();
            assertThat(images).extracting(ri -> ri.getImage().getId()).containsExactly(image.getId());
            assertThat(events.published).containsExactly(new CreateAdminAlertEvent(
                    team.startup.gwangsan.domain.admin.entity.constant.AlertType.REPORT, saved.getId(), reporter.getId()));
        }

        @Test
        @DisplayName("it_게시글 신고는 작성자를 피신고자로 기록하고 같은 작성자의 다른 게시글·회원 신고는 별개로 허용한다")
        void it_separates_product_and_member_duplicates() {
            MemberJpaEntity reporter = member("신고자");
            MemberJpaEntity owner = member("작성자");
            Product first = product(owner, ProductStatus.ONGOING);
            Product second = product(owner, ProductStatus.ONGOING);

            createReportService.execute(productReport(reporter, first));
            createReportService.execute(productReport(reporter, second));
            createReportService.execute(new CreateReportCommand(reporter.getId(), ReportTargetType.MEMBER, owner.getId(),
                    ReportType.SPAM_AD, "회원", null));

            assertThatThrownBy(() -> createReportService.execute(productReport(reporter, first)))
                    .isInstanceOf(AlreadyReportedException.class);
            assertThatThrownBy(() -> createReportService.execute(new CreateReportCommand(reporter.getId(),
                    ReportTargetType.MEMBER, owner.getId(), ReportType.SPAM_AD, "회원", null)))
                    .isInstanceOf(AlreadyReportedException.class);

            List<ReportJpaEntity> reports = em.getEntityManager()
                    .createQuery("select r from ReportJpaEntity r", ReportJpaEntity.class).getResultList();
            assertThat(reports).hasSize(3).allMatch(r -> r.getReported().getId().equals(owner.getId()));
        }

        @Test
        @DisplayName("it_삭제된 게시글과 없는 회원은 각 업무의 NotFound 예외로 거절한다")
        void it_rejects_missing_targets() {
            MemberJpaEntity reporter = member("신고자");
            Product deleted = product(member("작성자"), ProductStatus.DELETED);

            assertThatThrownBy(() -> createReportService.execute(productReport(reporter, deleted)))
                    .isInstanceOf(NotFoundProductException.class);
            assertThatThrownBy(() -> createReportService.execute(new CreateReportCommand(reporter.getId(),
                    ReportTargetType.MEMBER, 999_999L, ReportType.ETC, "없음", null)))
                    .isInstanceOf(NotFoundMemberException.class);
            assertThat(events.published).isEmpty();
        }
    }

    @Nested
    @DisplayName("차단은")
    class Describe_block {

        @Test
        @DisplayName("it_차단→중복 거절→양방향 판정→목록→해제→없는 차단 해제 거절 순서로 동작한다")
        void it_runs_block_lifecycle() {
            MemberJpaEntity blocker = member("차단자");
            MemberJpaEntity blocked = member("차단대상");

            blockMemberService.execute(new MemberBlockCommand(blocker.getId(), blocked.getId()));
            em.flush();

            assertThatThrownBy(() -> blockMemberService.execute(new MemberBlockCommand(blocker.getId(), blocked.getId())))
                    .isInstanceOf(AlreadyBlockedException.class);
            assertThatThrownBy(() -> blockQueryService.validateNotBlocked(blocked.getId(), blocker.getId()))
                    .isInstanceOf(BlockedMemberException.class);
            em.clear();
            assertThat(blockQueryService.query(blocker.getId()))
                    .containsExactly(new BlockedMember(blocked.getId(), blocked.getNickname()));
            assertThat(blockQueryService.query(blocked.getId())).isEmpty();

            unblockMemberService.execute(new MemberBlockCommand(blocker.getId(), blocked.getId()));
            em.flush();

            assertThat(em.getEntityManager().createQuery("select count(b) from MemberBlockJpaEntity b", Long.class)
                    .getSingleResult()).isZero();
            assertThatThrownBy(() -> unblockMemberService.execute(new MemberBlockCommand(blocker.getId(), blocked.getId())))
                    .isInstanceOf(NotFoundBlockException.class);
        }

        @Test
        @DisplayName("it_없는 회원은 차단하지 않는다")
        void it_rejects_missing_target() {
            MemberJpaEntity blocker = member("차단자");

            assertThatThrownBy(() -> blockMemberService.execute(new MemberBlockCommand(blocker.getId(), 999_999L)))
                    .isInstanceOf(NotFoundMemberException.class);
            assertThat(em.getEntityManager().createQuery("select b from MemberBlockJpaEntity b", MemberBlockJpaEntity.class)
                    .getResultList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("정지는")
    class Describe_suspend {

        @Test
        @DisplayName("it_신고 회원을 정지하고 알림을 요청한 뒤 기간이 지나면 해제한다")
        void it_suspends_and_releases() {
            MemberJpaEntity target = member("정지대상");

            suspendMemberService.execute(new SuspendMemberCommand(target.getId(), 3, 77L));
            em.flush();
            em.clear();

            SuspendJpaEntity suspend = em.getEntityManager()
                    .createQuery("select s from SuspendJpaEntity s", SuspendJpaEntity.class).getSingleResult();
            assertThat(suspend.getMember().getId()).isEqualTo(target.getId());
            assertThat(suspend.getSuspendedUntil()).isEqualTo(suspend.getSuspendedAt().plusDays(3));
            assertThat(em.find(MemberJpaEntity.class, target.getId()).getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            assertThat(events.published).containsExactly(new CreateAlertEvent(77L, target.getId(),
                    team.startup.gwangsan.domain.alert.model.AlertType.REPORT, suspend.getId()));

            assertThat(releaseExpiredSuspensionsService.execute(suspend.getSuspendedUntil())).isZero();
            assertThat(releaseExpiredSuspensionsService.execute(suspend.getSuspendedUntil().plusSeconds(1))).isEqualTo(1);
            em.flush();
            em.clear();
            assertThat(em.find(MemberJpaEntity.class, target.getId()).getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("it_과거 만료 정지가 있어도 진행 중인 재정지가 있으면 재정지 종료 시각까지 SUSPENDED 를 유지한다")
        void it_keeps_resuspended_member_until_its_boundary() {
            MemberJpaEntity target = member("재정지");
            LocalDateTime until = LocalDateTime.of(2026, 10, 20, 10, 0);
            suspension(target, until.minusDays(10));
            suspension(target, until);
            target.updateMemberStatus(MemberStatus.SUSPENDED);
            em.flush();
            em.clear();

            assertThat(releaseExpiredSuspensionsService.execute(until.minusDays(1))).isZero();
            assertThat(releaseExpiredSuspensionsService.execute(until)).isZero();
            assertThat(statusOf(target)).isEqualTo(MemberStatus.SUSPENDED);

            assertThat(releaseExpiredSuspensionsService.execute(until.plusSeconds(1))).isEqualTo(1);
            assertThat(statusOf(target)).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("it_진행 중인 정지만 있으면 SUSPENDED 를 유지한다")
        void it_keeps_member_with_only_active_suspension() {
            MemberJpaEntity target = member("진행중");
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 10, 0);
            suspension(target, now.plusMinutes(1));
            target.updateMemberStatus(MemberStatus.SUSPENDED);
            em.flush();
            em.clear();

            assertThat(releaseExpiredSuspensionsService.execute(now)).isZero();
            assertThat(statusOf(target)).isEqualTo(MemberStatus.SUSPENDED);
        }

        @Test
        @DisplayName("it_과거 정지 이력이 있어도 SUSPENDED 가 아닌 회원(PENDING·WITHDRAWN·ACTIVE)의 상태는 덮지 않는다")
        void it_does_not_overwrite_other_statuses() {
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 10, 0);
            MemberJpaEntity pending = member("대기");
            MemberJpaEntity withdrawn = member("탈퇴");
            MemberJpaEntity active = member("활성");
            for (MemberJpaEntity m : List.of(pending, withdrawn, active)) {
                suspension(m, now.minusDays(1));
            }
            pending.updateMemberStatus(MemberStatus.PENDING);
            withdrawn.updateMemberStatus(MemberStatus.WITHDRAWN);
            em.flush();
            em.clear();

            assertThat(releaseExpiredSuspensionsService.execute(now)).isZero();
            assertThat(statusOf(pending)).isEqualTo(MemberStatus.PENDING);
            assertThat(statusOf(withdrawn)).isEqualTo(MemberStatus.WITHDRAWN);
            assertThat(statusOf(active)).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("it_만료 정지가 여러 건이어도 회원을 한 번만 해제하고 정지 이력은 남긴다")
        void it_releases_once_and_keeps_history() {
            MemberJpaEntity target = member("여러건");
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 10, 0);
            suspension(target, now.minusDays(10));
            suspension(target, now.minusDays(1));
            target.updateMemberStatus(MemberStatus.SUSPENDED);
            em.flush();
            em.clear();

            assertThat(releaseExpiredSuspensionsService.execute(now)).isEqualTo(1);
            assertThat(statusOf(target)).isEqualTo(MemberStatus.ACTIVE);
            assertThat(em.getEntityManager().createQuery(
                            "select count(s) from SuspendJpaEntity s where s.member.id = :id", Long.class)
                    .setParameter("id", target.getId()).getSingleResult()).isEqualTo(2L);
        }

        @Test
        @DisplayName("it_없는 회원 정지는 NotFoundMemberException 으로 거절하고 정지 행을 만들지 않는다")
        void it_rejects_missing_member() {
            assertThatThrownBy(() -> suspendMemberService.execute(new SuspendMemberCommand(999_999L, 3, null)))
                    .isInstanceOf(NotFoundMemberException.class);
            assertThat(em.getEntityManager().createQuery("select count(s) from SuspendJpaEntity s", Long.class)
                    .getSingleResult()).isZero();
        }
    }

    @Nested
    @DisplayName("다른 업무용 공개 Port 는")
    class Describe_public_ports {

        @Test
        @DisplayName("it_신고 알림 원천·이미지 연결 행 ID·연결 삭제·정지 일수를 기존 의미로 제공한다")
        void it_serves_report_and_suspension_queries() {
            MemberJpaEntity reporter = member("공개신고자");
            MemberJpaEntity reported = member("공개피신고자");
            Image image = em.persist(Image.builder().imageUrl("https://img/p").build());
            createReportService.execute(new CreateReportCommand(reporter.getId(), ReportTargetType.MEMBER, reported.getId(),
                    ReportType.ETC, "공개", List.of(image.getId())));
            suspendMemberService.execute(new SuspendMemberCommand(reported.getId(), 5, null));
            em.flush();
            em.clear();
            ReportJpaEntity report = singleReport();
            ReportImageJpaEntity link = em.getEntityManager()
                    .createQuery("select ri from ReportImageJpaEntity ri", ReportImageJpaEntity.class).getSingleResult();
            SuspendJpaEntity suspend = em.getEntityManager()
                    .createQuery("select s from SuspendJpaEntity s", SuspendJpaEntity.class).getSingleResult();

            assertThat(reportQueryService.getAlertSource(report.getId()))
                    .isEqualTo(new ReportAlertSource(reporter.getId(), reported.getNickname()));
            assertThat(reportQueryService.findImageLinks(List.of(report.getId())))
                    .isEqualTo(java.util.Map.of(report.getId(), List.of(new ReportImageLink(link.getId(), "https://img/p"))));
            assertThat(reportQueryService.findImageLinks(List.of())).isEmpty();
            assertThat(suspensionQueryService.getSuspendedDays(suspend.getId())).isEqualTo(5);
            assertThatThrownBy(() -> reportQueryService.getAlertSource(999_999L)).isInstanceOf(NotFoundReportException.class);
            assertThatThrownBy(() -> suspensionQueryService.getSuspendedDays(999_999L)).isInstanceOf(NotFoundSuspendException.class);
            assertThatThrownBy(() -> suspensionQueryService.getSuspendedDays(null)).isInstanceOf(NotFoundSuspendException.class);

            reportQueryService.unlink(report.getId(), image.getId());
            em.flush();
            em.clear();
            assertThat(em.getEntityManager().createQuery("select count(ri) from ReportImageJpaEntity ri", Long.class)
                    .getSingleResult()).isZero();
            assertThat(em.find(Image.class, image.getId())).isNotNull();
        }
    }

    @Nested
    @DisplayName("정지 해제 트랜잭션은")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class Describe_release_rollback {

        @MockitoSpyBean private MemberStatusBridgeAdapter memberStatusBridgeAdapter;
        @Autowired private MemberJpaRepository memberRepository;
        @Autowired private SuspendJpaRepository suspendJpaRepository;
        @Autowired private PlatformTransactionManager transactionManager;
        @Autowired private ReleaseExpiredSuspensionsService releaseService;

        @AfterEach
        void cleanUp() {
            suspendJpaRepository.deleteAll();
            memberRepository.deleteAll();
        }

        @Test
        @DisplayName("it_중간 회원 해제가 실패하면 앞서 해제한 회원까지 모두 rollback 한다")
        void it_rolls_back_all_members_on_failure() {
            LocalDateTime now = LocalDateTime.of(2026, 10, 14, 10, 0);
            List<Long> memberIds = new TransactionTemplate(transactionManager).execute(status -> {
                MemberJpaEntity first = memberRepository.save(newMember("롤백1", MemberStatus.SUSPENDED));
                MemberJpaEntity second = memberRepository.save(newMember("롤백2", MemberStatus.SUSPENDED));
                for (MemberJpaEntity m : List.of(first, second)) {
                    suspendJpaRepository.save(SuspendJpaEntity.builder().member(m).suspendedAt(now.minusDays(2))
                            .suspendedDays(1).suspendedUntil(now.minusDays(1)).build());
                }
                return List.of(first.getId(), second.getId());
            });
            AtomicInteger calls = new AtomicInteger();
            doAnswer(invocation -> {
                if (calls.incrementAndGet() == 2) {
                    throw new IllegalStateException("두 번째 해제 실패");
                }
                return invocation.callRealMethod();
            }).when(memberStatusBridgeAdapter).activate(any());

            assertThatThrownBy(() -> releaseService.execute(now)).isInstanceOf(IllegalStateException.class);

            assertThat(memberRepository.findAllById(memberIds)).extracting(MemberJpaEntity::getStatus)
                    .containsOnly(MemberStatus.SUSPENDED);
        }
    }

    private CreateReportCommand productReport(MemberJpaEntity reporter, Product product) {
        return new CreateReportCommand(reporter.getId(), ReportTargetType.PRODUCT, product.getId(), ReportType.SPAM_AD, "광고", null);
    }

    private ReportJpaEntity singleReport() {
        return em.getEntityManager().createQuery("select r from ReportJpaEntity r", ReportJpaEntity.class).getSingleResult();
    }

    private void suspension(MemberJpaEntity member, LocalDateTime until) {
        em.persist(SuspendJpaEntity.builder().member(member).suspendedAt(until.minusDays(1)).suspendedDays(1)
                .suspendedUntil(until).build());
    }

    private MemberStatus statusOf(MemberJpaEntity member) {
        em.flush();
        em.clear();
        return em.find(MemberJpaEntity.class, member.getId()).getStatus();
    }

    private MemberJpaEntity member(String suffix) {
        return em.persist(newMember(suffix, MemberStatus.ACTIVE));
    }

    private static MemberJpaEntity newMember(String suffix, MemberStatus status) {
        return MemberJpaEntity.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-9" + String.format("%03d", Math.floorMod(suffix.hashCode(), 1000)) + "-" + System.nanoTime() % 10000)
                .role(MemberRole.ROLE_USER)
                .status(status)
                .build();
    }

    private Product product(MemberJpaEntity owner, ProductStatus status) {
        return em.persist(Product.builder().title("제목").description("설명").gwangsan(5000).member(owner)
                .status(status).type(Type.OBJECT).mode(Mode.GIVER).build());
    }
}
