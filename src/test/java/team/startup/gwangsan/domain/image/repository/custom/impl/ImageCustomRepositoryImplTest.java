package team.startup.gwangsan.domain.image.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@DisplayName("ImageCustomRepositoryImpl MariaDB integration test")
class ImageCustomRepositoryImplTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    @Autowired
    private DataSource dataSource;

    private ImageCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ImageCustomRepositoryImpl(queryFactory);
    }

    @Test
    @DisplayName("returns no candidates from an empty MariaDB database")
    void returnsNoCandidatesFromAnEmptyMariaDbDatabase() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).contains("MariaDB");
        }

        assertThat(repository.findAllOrphanImages(10)).extracting(Image::getId).isEmpty();
    }

    @Test
    @DisplayName("excludes images referenced only by chat messages or trade cancellations")
    void excludesImagesReferencedOnlyByChatMessagesOrTradeCancellations() {
        Relationships relationships = persistRelationships("chat-trade");
        Image chatOnly = image("chat-only");
        Image tradeCancelOnly = image("trade-cancel-only");
        em.persist(ChatMessageImage.builder().chatMessage(relationships.chatMessage()).image(chatOnly).build());
        em.persist(TradeCancelImage.builder().tradeCancel(relationships.tradeCancel()).image(tradeCancelOnly).build());
        em.flush();
        em.clear();

        assertThat(rowCount("tbl_chat_message_image")).isEqualTo(1);
        assertThat(rowCount("tbl_trade_cancel_image")).isEqualTo(1);
        assertThat(repository.findAllOrphanImages(10)).isEmpty();
    }

    @Test
    @DisplayName("keeps product notice and report images out before limiting candidates")
    void keepsProductNoticeAndReportImagesOutBeforeLimitingCandidates() {
        Relationships relationships = persistRelationships("existing-links");
        Image productOnly = image("product-only");
        Image noticeOnly = image("notice-only");
        Image reportOnly = image("report-only");
        Image firstOrphan = image("first-orphan");
        Image secondOrphan = image("second-orphan");
        em.persist(ProductImage.builder().product(relationships.product()).image(productOnly).build());
        em.persist(NoticeImage.builder().notice(relationships.notice()).image(noticeOnly).build());
        em.persist(ReportImage.builder().report(relationships.report()).image(reportOnly).build());
        em.flush();

        LocalDateTime protectedTime = LocalDateTime.of(2024, 1, 1, 9, 0);
        updateCreatedAt(productOnly, protectedTime);
        updateCreatedAt(noticeOnly, protectedTime);
        updateCreatedAt(reportOnly, protectedTime);
        updateCreatedAt(firstOrphan, LocalDateTime.of(2024, 1, 1, 10, 0));
        updateCreatedAt(secondOrphan, LocalDateTime.of(2024, 1, 1, 10, 1));
        em.clear();

        assertThat(repository.findAllOrphanImages(10)).extracting(Image::getId)
                .containsExactly(firstOrphan.getId(), secondOrphan.getId());
        assertThat(repository.findAllOrphanImages(1)).extracting(Image::getId)
                .containsExactly(firstOrphan.getId());
    }

    @Test
    @DisplayName("returns each true orphan once in created-at and id order within the limit")
    void returnsEachTrueOrphanOnceInCreatedAtAndIdOrderWithinTheLimit() {
        Relationships relationships = persistRelationships("all-links");
        Image productOnly = image("product-only");
        Image noticeOnly = image("notice-only");
        Image reportOnly = image("report-only");
        Image chatOnly = image("chat-only");
        Image tradeCancelOnly = image("trade-cancel-only");
        Image multipleReferences = image("multiple-references");
        Image later = image("later");
        Image earlier = image("earlier");
        Image sameTimeFirst = image("same-time-first");
        Image sameTimeSecond = image("same-time-second");

        em.persist(ProductImage.builder().product(relationships.product()).image(productOnly).build());
        em.persist(NoticeImage.builder().notice(relationships.notice()).image(noticeOnly).build());
        em.persist(ReportImage.builder().report(relationships.report()).image(reportOnly).build());
        em.persist(ChatMessageImage.builder().chatMessage(relationships.chatMessage()).image(chatOnly).build());
        em.persist(TradeCancelImage.builder().tradeCancel(relationships.tradeCancel()).image(tradeCancelOnly).build());
        em.persist(ProductImage.builder().product(relationships.product()).image(multipleReferences).build());
        em.persist(ProductImage.builder().product(relationships.product()).image(multipleReferences).build());
        em.persist(ReportImage.builder().report(relationships.report()).image(multipleReferences).build());
        em.flush();

        LocalDateTime protectedTime = LocalDateTime.of(2024, 1, 1, 9, 0);
        updateCreatedAt(productOnly, protectedTime);
        updateCreatedAt(noticeOnly, protectedTime);
        updateCreatedAt(reportOnly, protectedTime);
        updateCreatedAt(chatOnly, protectedTime);
        updateCreatedAt(tradeCancelOnly, protectedTime);
        updateCreatedAt(multipleReferences, protectedTime);
        updateCreatedAt(later, LocalDateTime.of(2024, 1, 1, 10, 2));
        updateCreatedAt(earlier, LocalDateTime.of(2024, 1, 1, 10, 1));
        LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 10, 3);
        updateCreatedAt(sameTimeFirst, sameTime);
        updateCreatedAt(sameTimeSecond, sameTime);
        em.clear();

        assertThat(repository.findAllOrphanImages(10)).extracting(Image::getId)
                .containsExactly(earlier.getId(), later.getId(), sameTimeFirst.getId(), sameTimeSecond.getId());
        assertThat(repository.findAllOrphanImages(1)).extracting(Image::getId)
                .containsExactly(earlier.getId());
        assertThat(repository.findAllOrphanImages(100)).extracting(Image::getId)
                .containsExactly(earlier.getId(), later.getId(), sameTimeFirst.getId(), sameTimeSecond.getId());
    }

    private Relationships persistRelationships(String suffix) {
        Member buyer = member("buyer-" + suffix, "010-0000-" + suffix.hashCode());
        Member seller = member("seller-" + suffix, "010-0001-" + suffix.hashCode());
        Product product = em.persist(Product.builder()
                .title("product-" + suffix)
                .description("description")
                .gwangsan(5_000)
                .member(seller)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .status(ProductStatus.ONGOING)
                .build());
        Head head = em.persist(new Head("head-" + suffix));
        Place place = em.persist(Place.builder().name("place-" + suffix).head(head).build());
        Notice notice = em.persist(Notice.builder().title("notice").content("content").place(place).member(seller).build());
        Report report = em.persist(Report.builder()
                .reportType(ReportType.ETC)
                .content("content")
                .reported(seller)
                .reporter(buyer)
                .build());
        TradeComplete tradeComplete = em.persist(TradeComplete.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .status(TradeStatus.COMPLETED)
                .requestedBySeller(true)
                .build());
        TradeCancel tradeCancel = em.persist(TradeCancel.builder()
                .tradeComplete(tradeComplete)
                .member(buyer)
                .reason("reason")
                .status(TradeCancelStatus.PENDING)
                .build());
        ChatRoom chatRoom = em.persist(ChatRoom.builder().buyer(buyer).seller(seller).product(product).isActive(true).build());
        ChatMessage chatMessage = em.persist(ChatMessage.builder()
                .id((long) suffix.hashCode())
                .content("image")
                .messageType(MessageType.IMAGE)
                .checked(false)
                .room(chatRoom)
                .sender(buyer)
                .createdAt(LocalDateTime.of(2024, 1, 1, 9, 0))
                .build());
        return new Relationships(product, notice, report, chatMessage, tradeCancel);
    }

    private Member member(String nickname, String phoneNumber) {
        return em.persist(Member.builder()
                .name(nickname)
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phoneNumber)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Image image(String name) {
        return em.persist(Image.builder().imageUrl("https://test.local/" + name).build());
    }

    private long rowCount(String table) {
        return ((Number) em.getEntityManager().createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }

    private void updateCreatedAt(Image image, LocalDateTime createdAt) {
        em.getEntityManager().createNativeQuery("update tbl_image set created_at = ? where image_id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, image.getId())
                .executeUpdate();
    }

    private record Relationships(Product product, Notice notice, Report report, ChatMessage chatMessage, TradeCancel tradeCancel) {
    }
}
