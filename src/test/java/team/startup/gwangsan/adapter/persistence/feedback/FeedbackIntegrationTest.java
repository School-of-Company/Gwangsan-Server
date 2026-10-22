package team.startup.gwangsan.adapter.persistence.feedback;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import team.startup.gwangsan.adapter.persistence.member.*;
import team.startup.gwangsan.adapter.persistence.place.*;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.image.*;
import team.startup.gwangsan.adapter.persistence.alert.AlertPersistenceAdapter;
import team.startup.gwangsan.adapter.integration.feedback.FeedbackReferencesAdapter;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEventListener;
import team.startup.gwangsan.application.feedback.service.*;
import team.startup.gwangsan.application.feedback.data.*;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.alert.service.CreateAlertService;
import team.startup.gwangsan.application.alert.port.out.*;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.*;
import team.startup.gwangsan.domain.feedback.exception.*;
import team.startup.gwangsan.domain.alert.model.AlertType;
import team.startup.gwangsan.domain.member.model.*;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.show-sql=false"})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, FeedbackPersistenceAdapter.class, FeedbackService.class, CreateFeedbackService.class,
        FeedbackReferencesAdapter.class, CreateAlertService.class, AlertPersistenceAdapter.class,
        PushRequestedEventListener.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("문의 MariaDB 저장·권한·트랜잭션·정리 통합 검증")
class FeedbackIntegrationTest {
    @Container static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mariadb::getJdbcUrl);
        r.add("spring.datasource.username", mariadb::getUsername);
        r.add("spring.datasource.password", mariadb::getPassword);
        r.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired FeedbackPersistenceAdapter port;
    @Autowired FeedbackService service;
    @Autowired CreateFeedbackService create;
    @Autowired AlertPersistenceAdapter alerts;
    @Autowired JPAQueryFactory queries;
    @Autowired DataSource dataSource;
    @MockitoBean MemberSummaryQuery members;
    @MockitoBean ImageQuery images;
    @MockitoBean LoadAlertSourcePort sources;
    @MockitoBean SendPushUseCase push;
    private TransactionTemplate tx;

    void migrateFromPreviousSchema() throws Exception {
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).baselineVersion("12").target("13").load();
        if (flyway.info().current() != null) return;
        try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.execute("DROP TABLE tbl_feedback_image");
            sql.execute("DROP TABLE tbl_feedback");
            sql.execute("""
                    ALTER TABLE tbl_alert MODIFY COLUMN alert_type
                    ENUM('NOTICE','TRADE_COMPLETE','TRADE_COMPLETE_REJECT','OTHER_MEMBER_TRADE_COMPLETE',
                    'RECOMMENDER','REVIEW','TRADE_CANCEL','TRADE_CANCEL_REJECT','REPORT','REPORT_REJECT') NOT NULL
                    """);
        }
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();
    }

    @BeforeEach
    void setUp() throws Exception {
        migrateFromPreviousSchema();
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            for (String table : List.of("tbl_feedback_image", "tbl_feedback", "tbl_alert_receipt", "tbl_alert",
                    "tbl_member_detail", "tbl_member", "tbl_place", "tbl_dong", "tbl_head_place")) {
                em.createNativeQuery("delete from " + table).executeUpdate();
            }
        });
    }

    @Test
    void it_queries_own_records_and_scope_with_filters_and_cursor() {
        tx.executeWithoutResult(s -> {
            MemberJpaEntity local = member("local");
            MemberJpaEntity other = member("other");
            Integer placeId = place(local, "local").getId();
            place(other, "other");
            stubMember(local, placeId);
            List<Long> expected = new ArrayList<>();
            for (FeedbackType type : FeedbackType.values()) {
                create.execute(command(local.getId(), type, List.of()));
                expected.add(latest(local.getId()));
                create.execute(command(other.getId(), type, List.of()));
                if (type == FeedbackType.BUG) expected.add(latest(other.getId()));
            }
            em.flush(); em.clear();
            ActorContext admin = new ActorContext(local.getId(), ActorContext.Role.ROLE_PLACE_ADMIN);
            assertThat(service.list(admin, null, null, 999, null, 100))
                    .extracting(d -> d.feedback().id()).containsExactlyElementsOf(expected.reversed());
            assertThat(service.mine(local.getId(), null, 100)).hasSize(5)
                    .allMatch(d -> d.feedback().memberId().equals(local.getId()));
            var first = service.list(admin, null, null, null, null, 2);
            var next = service.list(admin, null, null, null, first.getLast().feedback().id(), 100);
            assertThat(next).extracting(d -> d.feedback().id())
                    .doesNotContainAnyElementsOf(first.stream().map(d -> d.feedback().id()).toList()).hasSize(4);
            assertThat(service.list(admin, FeedbackStatus.RECEIVED, FeedbackType.BUG, null, null, 100)).hasSize(2);
            ActorContext head = new ActorContext(local.getId(), ActorContext.Role.ROLE_HEAD_ADMIN);
            assertThat(service.list(head, null, null, null, null, 100)).hasSizeGreaterThanOrEqualTo(10);
            assertThat(service.list(head, null, null, placeId, null, 100)).hasSize(5);
        });
    }

    @Test
    void it_commits_answer_alert_and_push_then_preserves_answer_on_status_change() {
        Long[] ids = tx.execute(s -> {
            MemberJpaEntity author = member("author");
            MemberJpaEntity admin = member("admin");
            create.execute(command(author.getId(), FeedbackType.BUG, List.of()));
            return new Long[]{latest(author.getId()), author.getId(), admin.getId()};
        });
        ActorContext admin = new ActorContext(ids[2], ActorContext.Role.ROLE_HEAD_ADMIN);
        tx.executeWithoutResult(s -> {
            service.answer(admin, ids[0], "첫 답변");
            verifyNoInteractions(push);
        });
        verify(push).send(argThat(c -> c.recipientMemberIds().equals(List.of(ids[1]))
                && c.message().sourceId().equals(ids[0]) && c.message().type().name().equals("FEEDBACK_ANSWER")));
        tx.executeWithoutResult(s -> {
            FeedbackDetail result = service.detail(admin, ids[0]);
            assertThat(result.feedback().answer()).isEqualTo("첫 답변");
            assertThat(result.feedback().status()).isEqualTo(FeedbackStatus.DONE);
            assertThat(result.feedback().answeredAt()).isNotNull();
            assertThat(result.answeredBy()).startsWith("admin");
            assertThat(alerts.loadReceived(ids[1])).singleElement().satisfies(a -> {
                assertThat(a.type()).isEqualTo(AlertType.FEEDBACK_ANSWER);
                assertThat(a.sourceId()).isEqualTo(ids[0]);
            });
            service.changeStatus(admin, ids[0], FeedbackStatus.IN_PROGRESS);
        });
        reset(push);
        tx.executeWithoutResult(s -> service.answer(admin, ids[0], "수정 답변"));
        tx.executeWithoutResult(s -> {
            assertThat(service.detail(admin, ids[0]).feedback().answer()).isEqualTo("수정 답변");
            assertThat(alerts.loadReceived(ids[1])).hasSize(2);
        });
        verify(push).send(any());
    }

    @Test
    void it_rolls_back_answer_and_alert_without_sending_push() {
        Long[] ids = tx.execute(s -> {
            MemberJpaEntity author = member("rollback");
            MemberJpaEntity admin = member("rollbackAdmin");
            create.execute(command(author.getId(), FeedbackType.BUG, List.of()));
            return new Long[]{latest(author.getId()), author.getId(), admin.getId()};
        });
        ActorContext admin = new ActorContext(ids[2], ActorContext.Role.ROLE_HEAD_ADMIN);
        tx.executeWithoutResult(s -> {
            service.answer(admin, ids[0], "롤백");
            em.flush();
            s.setRollbackOnly();
        });
        verifyNoInteractions(push);
        tx.executeWithoutResult(s -> {
            assertThat(service.detail(admin, ids[0]).feedback().answer()).isNull();
            assertThat(alerts.loadReceived(ids[1])).isEmpty();
        });
    }

    @Test
    void it_keeps_attached_images_and_reassigns_author_and_answering_admin_on_withdrawal() {
        tx.executeWithoutResult(s -> {
            MemberJpaEntity author = member("cleanup");
            MemberJpaEntity dummy = member("dummy");
            ImageJpaEntity image = new ImageJpaEntity("https://feedback-image");
            em.persist(image);
            when(images.findAllByIds(List.of(image.getId())))
                    .thenReturn(List.of(new ImageSummary(image.getId(), image.getImageUrl())));
            create.execute(command(author.getId(), FeedbackType.ETC, List.of(image.getId())));
            Long id = latest(author.getId());
            ActorContext actor = new ActorContext(author.getId(), ActorContext.Role.ROLE_HEAD_ADMIN);
            service.answer(actor, id, "답변");
            em.flush(); em.clear();
            assertThat(new OrphanImageQueryAdapter(queries).findOrphanCandidates(100))
                    .extracting(i -> i.id()).doesNotContain(image.getId());
            service.reassign(author.getId(), dummy.getId());
            Feedback reassigned = port.find(id).orElseThrow().feedback();
            assertThat(reassigned.memberId()).isEqualTo(dummy.getId());
            assertThat(reassigned.answeredBy()).isEqualTo(dummy.getId());
            service.unlink(id, image.getId());
            em.flush(); em.clear();
            assertThat(port.find(id).orElseThrow().images()).isEmpty();
            assertThat(new OrphanImageQueryAdapter(queries).findOrphanCandidates(100))
                    .extracting(i -> i.id()).contains(image.getId());
            alerts.reassignSender(author.getId(), dummy.getId());
            alerts.deleteReceipts(author.getId());
            em.remove(em.find(MemberJpaEntity.class, author.getId()));
            em.flush();
            s.setRollbackOnly();
        });
    }

    @Test
    void it_rejects_unknown_images_before_persisting() {
        tx.executeWithoutResult(s -> {
            MemberJpaEntity author = member("invalidImage");
            when(images.findAllByIds(List.of(999L))).thenReturn(List.of());
            assertThatThrownBy(() -> create.execute(command(author.getId(), FeedbackType.BUG, List.of(999L))))
                    .isInstanceOf(InvalidFeedbackException.class);
            assertThat(service.mine(author.getId(), null, 10)).isEmpty();
            s.setRollbackOnly();
        });
    }

    private Long latest(Long memberId) { return service.mine(memberId, null, 1).getFirst().feedback().id(); }

    private CreateFeedbackCommand command(Long memberId, FeedbackType type, List<Long> imageIds) {
        return new CreateFeedbackCommand(memberId, type, "문의", imageIds, "1.2.2", OsType.IOS, "18", "iPhone");
    }

    private MemberJpaEntity member(String name) {
        String unique = name + UUID.randomUUID().toString().substring(0, 8);
        MemberJpaEntity member = new MemberJpaEntity(unique, unique, unique, "pw", null,
                MemberRole.ROLE_USER, MemberStatus.ACTIVE);
        em.persist(member);
        stubMember(member, null);
        return member;
    }

    private void stubMember(MemberJpaEntity m, Integer placeId) {
        when(members.findById(m.getId())).thenReturn(Optional.of(new MemberSummary(m.getId(), m.getNickname(),
                m.getPhoneNumber(), null, null, placeId, null, null, 0)));
        when(images.findAllByIds(List.of())).thenReturn(List.of());
    }

    private PlaceJpaEntity place(MemberJpaEntity member, String name) {
        String unique = name + UUID.randomUUID().toString().substring(0, 8);
        HeadJpaEntity head = new HeadJpaEntity(unique);
        em.persist(head);
        PlaceJpaEntity place = new PlaceJpaEntity(unique, head);
        em.persist(place);
        DongJpaEntity dong = new DongJpaEntity(unique);
        em.persist(dong);
        em.persist(new MemberDetailJpaEntity(member, dong, 0, place, 0, ""));
        return place;
    }
}
