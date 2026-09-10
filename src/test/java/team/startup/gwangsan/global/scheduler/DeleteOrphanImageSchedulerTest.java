package team.startup.gwangsan.global.scheduler;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.entity.dto.DeleteResult;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
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
import team.startup.gwangsan.global.thirdparty.aws.s3.service.S3DeleteService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, DeleteOrphanImageScheduler.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("DeleteOrphanImageScheduler MariaDB integration test")
class DeleteOrphanImageSchedulerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

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

    @Autowired private DeleteOrphanImageScheduler scheduler;
    @Autowired private ImageRepository imageRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationContext applicationContext;

    @MockitoBean private S3DeleteService s3DeleteService;

    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
        reset(s3DeleteService);
        assertThat(applicationContext.getBeanProvider(S3AsyncClient.class).getIfAvailable()).isNull();
        assertThat(applicationContext.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
        inTransaction(() -> {
            assertThat(rowCount("tbl_image")).isZero();
            assertThat(rowCount("tbl_product_image")).isZero();
            assertThat(rowCount("tbl_notice_image")).isZero();
            assertThat(rowCount("tbl_report_image")).isZero();
            assertThat(rowCount("tbl_chat_message_image")).isZero();
            assertThat(rowCount("tbl_trade_cancel_image")).isZero();
            return null;
        });
    }

    @AfterEach
    void cleanUpCommittedFixtures() {
        inTransaction(() -> {
            delete("tbl_chat_message_image");
            delete("tbl_trade_cancel_image");
            delete("tbl_product_image");
            delete("tbl_notice_image");
            delete("tbl_report_image");
            delete("tbl_chat_message");
            delete("tbl_chat_room");
            delete("tbl_trade_cancel");
            delete("tbl_trade_complete");
            delete("tbl_notice");
            delete("tbl_report");
            delete("tbl_product");
            delete("tbl_place");
            delete("tbl_head_place");
            delete("tbl_member");
            delete("tbl_image");
            return null;
        });
    }

    @Test
    @DisplayName("deletes only old unreferenced images and preserves all references and recent images")
    void deletesOnlyOldUnreferencedImages() {
        List<ImageFixture> fixtures = inTransaction(() -> {
            Relationships relationships = persistRelationships();
            List<ImageFixture> protectedImages = List.of(
                    image("product"), image("notice"), image("report"), image("chat"), image("trade-cancel"));
            entityManager.persist(ProductImage.builder().product(relationships.product()).image(reference(protectedImages, 0)).build());
            entityManager.persist(NoticeImage.builder().notice(relationships.notice()).image(reference(protectedImages, 1)).build());
            entityManager.persist(ReportImage.builder().report(relationships.report()).image(reference(protectedImages, 2)).build());
            entityManager.persist(ChatMessageImage.builder().chatMessage(relationships.message()).image(reference(protectedImages, 3)).build());
            entityManager.persist(TradeCancelImage.builder().tradeCancel(relationships.cancel()).image(reference(protectedImages, 4)).build());
            ImageFixture oldOrphan = image("old-orphan");
            ImageFixture recentOrphan = image("recent-orphan");
            entityManager.flush();
            protectedImages.forEach(image -> updateCreatedAt(image, oldTime()));
            updateCreatedAt(oldOrphan, oldTime());
            updateCreatedAt(recentOrphan, recentTime());
            entityManager.clear();
            List<ImageFixture> result = new ArrayList<>(protectedImages);
            result.add(oldOrphan);
            result.add(recentOrphan);
            return result;
        });
        ImageFixture oldOrphan = fixtures.get(5);
        ImageFixture recentOrphan = fixtures.get(6);
        AtomicReference<List<String>> keys = successfulS3();

        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        scheduler.deleteOrphanImages();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

        assertThat(keys.get()).containsExactly(oldOrphan.url());
        assertThat(imageExists(oldOrphan.id())).isFalse();
        assertThat(fixtures.subList(0, 5)).allSatisfy(image -> assertThat(imageExists(image.id())).isTrue());
        assertThat(imageExists(recentOrphan.id())).isTrue();
    }

    @Test
    @DisplayName("keeps failed S3 images while deleting successful old orphan images")
    void keepsFailedS3ImagesWhileDeletingSuccessfulOldOrphans() {
        List<ImageFixture> images = inTransaction(() -> oldImages("mixed", 2));
        when(s3DeleteService.deleteAll(anyList())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new DeleteResult(1, 1, List.of(images.get(1).url()));
        });

        scheduler.deleteOrphanImages();

        assertThat(imageExists(images.get(0).id())).isFalse();
        assertThat(imageExists(images.get(1).id())).isTrue();
        verify(s3DeleteService).deleteAll(List.of(images.get(0).url(), images.get(1).url()));
    }

    @Test
    @DisplayName("keeps every old orphan when every S3 delete fails")
    void keepsEveryOldOrphanWhenEveryS3DeleteFails() {
        List<ImageFixture> images = inTransaction(() -> oldImages("all-fail", 2));
        when(s3DeleteService.deleteAll(anyList())).thenReturn(new DeleteResult(0, 2, images.stream().map(ImageFixture::url).toList()));

        scheduler.deleteOrphanImages();

        assertThat(imageExists(images.get(0).id())).isTrue();
        assertThat(imageExists(images.get(1).id())).isTrue();
        verify(s3DeleteService).deleteAll(List.of(images.get(0).url(), images.get(1).url()));
    }

    @Test
    @DisplayName("does not call S3 for empty or recent-only candidate sets")
    void doesNotCallS3ForEmptyOrRecentOnlyCandidateSets() {
        scheduler.deleteOrphanImages();
        verifyNoInteractions(s3DeleteService);

        ImageFixture recent = inTransaction(() -> {
            ImageFixture fixture = image("recent-only");
            entityManager.flush();
            updateCreatedAt(fixture, recentTime());
            entityManager.clear();
            return fixture;
        });
        scheduler.deleteOrphanImages();

        verifyNoInteractions(s3DeleteService);
        assertThat(imageExists(recent.id())).isTrue();
    }

    @Test
    @DisplayName("keeps database images when the S3 double throws")
    void keepsDatabaseImagesWhenS3Throws() {
        ImageFixture image = inTransaction(() -> oldImages("throws", 1).getFirst());
        doThrow(new IllegalStateException("S3 unavailable")).when(s3DeleteService).deleteAll(anyList());

        scheduler.deleteOrphanImages();

        assertThat(imageExists(image.id())).isTrue();
        verify(s3DeleteService).deleteAll(List.of(image.url()));
    }

    @Test
    @DisplayName("deletes the first 500 old orphans in id order and leaves the 501st")
    void deletesTheFirstFiveHundredOldOrphansInIdOrder() {
        List<ImageFixture> images = inTransaction(() -> oldImages("batch", 501));
        AtomicReference<List<String>> keys = successfulS3();

        scheduler.deleteOrphanImages();

        assertThat(keys.get()).containsExactlyElementsOf(images.subList(0, 500).stream().map(ImageFixture::url).toList());
        assertThat(images.subList(0, 500)).allSatisfy(image -> assertThat(imageExists(image.id())).isFalse());
        assertThat(imageExists(images.get(500).id())).isTrue();
    }

    private AtomicReference<List<String>> successfulS3() {
        AtomicReference<List<String>> keys = new AtomicReference<>();
        when(s3DeleteService.deleteAll(anyList())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            List<String> requestedKeys = List.copyOf(invocation.getArgument(0));
            keys.set(requestedKeys);
            return new DeleteResult(requestedKeys.size(), 0, List.of());
        });
        return keys;
    }

    private List<ImageFixture> oldImages(String prefix, int count) {
        List<ImageFixture> images = new ArrayList<>();
        LocalDateTime createdAt = oldTime();
        for (int index = 0; index < count; index++) {
            images.add(image(prefix + "-" + index));
        }
        entityManager.flush();
        images.forEach(image -> updateCreatedAt(image, createdAt));
        entityManager.clear();
        return images;
    }

    private Relationships persistRelationships() {
        Member buyer = member("buyer", "010-1000-0001");
        Member seller = member("seller", "010-1000-0002");
        Product product = persist(Product.builder().title("product").description("description").gwangsan(5_000)
                .member(seller).type(Type.SERVICE).mode(Mode.GIVER).status(ProductStatus.ONGOING).build());
        Head head = persist(new Head("head"));
        Place place = persist(Place.builder().name("place").head(head).build());
        Notice notice = persist(Notice.builder().title("notice").content("content").place(place).member(seller).build());
        Report report = persist(Report.builder().reportType(ReportType.ETC).content("content").reported(seller).reporter(buyer).build());
        TradeComplete complete = persist(TradeComplete.builder().product(product).buyer(buyer).seller(seller)
                .status(TradeStatus.COMPLETED).requestedBySeller(true).build());
        TradeCancel cancel = persist(TradeCancel.builder().tradeComplete(complete).member(buyer).reason("reason")
                .status(TradeCancelStatus.PENDING).build());
        ChatRoom room = persist(ChatRoom.builder().buyer(buyer).seller(seller).product(product).isActive(true).build());
        ChatMessage message = persist(ChatMessage.builder().id(1L).content("image").messageType(MessageType.IMAGE)
                .checked(false).room(room).sender(buyer).createdAt(oldTime()).build());
        return new Relationships(product, notice, report, message, cancel);
    }

    private Member member(String nickname, String phoneNumber) {
        return persist(Member.builder().name(nickname).nickname(nickname).password("pw").phoneNumber(phoneNumber)
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build());
    }

    private ImageFixture image(String name) {
        Image image = persist(Image.builder().imageUrl("scheduler/" + name).build());
        return new ImageFixture(image, image.getId(), image.getImageUrl());
    }

    private Image reference(List<ImageFixture> images, int index) {
        return images.get(index).entity();
    }

    private void updateCreatedAt(ImageFixture image, LocalDateTime createdAt) {
        entityManager.createNativeQuery("update tbl_image set created_at = ? where image_id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, image.id())
                .executeUpdate();
    }

    private boolean imageExists(Long imageId) {
        return inTransaction(() -> imageRepository.existsById(imageId));
    }

    private long rowCount(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private LocalDateTime oldTime() {
        return LocalDateTime.now(SEOUL).minusDays(3);
    }

    private LocalDateTime recentTime() {
        return LocalDateTime.now(SEOUL).minusDays(1);
    }

    private <T> T inTransaction(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }

    private void delete(String table) {
        entityManager.createNativeQuery("delete from " + table).executeUpdate();
    }

    private record ImageFixture(Image entity, Long id, String url) {
    }

    private record Relationships(Product product, Notice notice, Report report, ChatMessage message, TradeCancel cancel) {
    }
}
