package team.startup.gwangsan.adapter.persistence.notice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import team.startup.gwangsan.adapter.integration.notice.NoticeImageAdapter;
import team.startup.gwangsan.adapter.integration.notice.NoticeMemberAdapter;
import team.startup.gwangsan.adapter.integration.notice.NoticePlaceAdapter;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.application.notice.data.NoticePlace;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.image.ImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlacePersistenceAdapter;
import team.startup.gwangsan.application.image.service.ImageQueryService;
import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.place.service.PlaceQueryService;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.notice.model.Notice;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, NoticePersistenceAdapter.class,
        NoticePlaceAdapter.class, NoticeMemberAdapter.class, NoticeImageAdapter.class,
        PlaceQueryService.class, PlacePersistenceAdapter.class, ImageQueryService.class, ImagePersistenceAdapter.class})
@DisplayName("공지 persistence/bridge adapter MariaDB 통합 테스트")
class NoticePersistenceAdapterTest {

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

    @Autowired private TestEntityManager em;
    @Autowired private NoticePersistenceAdapter adapter;
    @Autowired private NoticePlaceAdapter placeAdapter;
    @Autowired private NoticeMemberAdapter memberAdapter;
    @Autowired private NoticeImageAdapter imageAdapter;
    @Autowired private NoticeJpaRepository noticeJpaRepository;
    @Autowired private NoticeImageJpaRepository noticeImageJpaRepository;

    private HeadJpaEntity head;
    private PlaceJpaEntity place;
    private PlaceJpaEntity sibling;
    private PlaceJpaEntity otherHeadPlace;
    private MemberJpaEntity author;

    @BeforeEach
    void setUp() {
        head = em.persist(new HeadJpaEntity("head"));
        place = em.persist(PlaceJpaEntity.builder().name("광산구").head(head).build());
        sibling = em.persist(PlaceJpaEntity.builder().name("수완동").head(head).build());
        otherHeadPlace = em.persist(PlaceJpaEntity.builder().name("다른 본부").head(em.persist(new HeadJpaEntity("other"))).build());
        author = member("author", "01000000001");
        em.persist(MemberDetailJpaEntity.builder().member(author).dong(em.persist(new DongJpaEntity("신가동"))).place(place).gwangsan(0).light(1).description("").build());
        em.flush();
    }

    @Nested
    @DisplayName("공지 저장은")
    class Describe_save {

        @Test
        @DisplayName("새 공지에 id·createdAt 을 채우고 수정은 같은 행을 갱신한다")
        void it_inserts_then_updates_same_row() {
            Notice saved = adapter.save(Notice.create("제목", "내용", place.getId(), author.getId()));
            em.flush();
            em.clear();

            assertThat(saved.id()).isNotNull();
            assertThat(saved.createdAt()).isNotNull();
            Notice reloaded = adapter.findById(saved.id()).orElseThrow();
            assertThat(reloaded).isEqualTo(new Notice(saved.id(), saved.title(), saved.content(),
                    saved.placeId(), saved.authorId(), saved.createdAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS)));

            adapter.save(new Notice(saved.id(), "새 제목", "새 내용", saved.placeId(), saved.authorId(), saved.createdAt()));
            em.flush();
            em.clear();

            Notice updated = adapter.findById(saved.id()).orElseThrow();
            assertThat(updated.title()).isEqualTo("새 제목");
            assertThat(updated.content()).isEqualTo("새 내용");
            assertThat(noticeJpaRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미지 연결 추가·교체·삭제와 공지 삭제가 실제 행에 반영된다")
        void it_adds_replaces_and_deletes_images() {
            ImageJpaEntity first = image("first");
            ImageJpaEntity second = image("second");
            Notice notice = adapter.save(Notice.create("제목", "내용", place.getId(), author.getId()));
            adapter.addImages(notice.id(), List.of(first.getId(), second.getId()));
            em.flush();
            em.clear();

            assertThat(adapter.findImages(notice.id())).containsExactly(
                    new NoticeImageResult(first.getId(), first.getImageUrl()),
                    new NoticeImageResult(second.getId(), second.getImageUrl()));

            adapter.deleteImages(notice.id());
            adapter.addImages(notice.id(), List.of(second.getId()));
            em.flush();
            em.clear();
            assertThat(adapter.findImages(notice.id())).extracting(NoticeImageResult::imageId).containsExactly(second.getId());

            adapter.delete(notice.id());
            em.flush();
            assertThat(noticeJpaRepository.count()).isZero();
            assertThat(noticeImageJpaRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("공지 목록 조회는")
    class Describe_findPage {

        @Test
        @DisplayName("지점 목록의 공지를 id 내림차순 cursor 로 size 개 돌려준다")
        void it_pages_by_cursor() {
            Long first = notice(place).id();
            Long second = notice(sibling).id();
            Long third = notice(place).id();
            notice(otherHeadPlace);
            em.flush();
            em.clear();

            assertThat(adapter.findPage(List.of(place.getId(), sibling.getId()), null, 2))
                    .extracting(Notice::id).containsExactly(third, second);
            assertThat(adapter.findPage(List.of(place.getId(), sibling.getId()), second, 2))
                    .extracting(Notice::id).containsExactly(first);
            assertThat(adapter.findPage(List.of(place.getId()), null, 10))
                    .extracting(Notice::id).containsExactly(third, first);
        }

        @Test
        @DisplayName("목록 이미지는 공지 id 로 걸러 공지별로 묶는다 (공지 id ≠ 연결 id)")
        void it_groups_images_by_notice_id() {
            // 선행 빈 공지로 공지 id 와 notice_image_id 가 겹치지 않게 한다.
            for (int i = 0; i < 5; i++) {
                notice(place);
            }
            Notice first = notice(place);
            Notice second = notice(place);
            Notice other = notice(place);
            ImageJpaEntity firstA = image("first-a");
            ImageJpaEntity firstB = image("first-b");
            ImageJpaEntity secondA = image("second-a");
            ImageJpaEntity otherA = image("other-a");
            adapter.addImages(first.id(), List.of(firstA.getId(), firstB.getId()));
            adapter.addImages(second.id(), List.of(secondA.getId()));
            adapter.addImages(other.id(), List.of(otherA.getId()));
            em.flush();
            em.clear();
            List<Long> linkIds = noticeImageJpaRepository.findAll().stream().map(NoticeImageJpaEntity::getId).toList();
            assertThat(linkIds).doesNotContain(first.id(), second.id());

            Map<Long, List<NoticeImageLink>> result = adapter.findImageLinks(List.of(first.id(), second.id()));

            assertThat(result).containsOnlyKeys(first.id(), second.id());
            assertThat(result.get(first.id())).extracting(NoticeImageLink::imageId, NoticeImageLink::imageUrl)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(firstA.getId(), firstA.getImageUrl()),
                            org.assertj.core.groups.Tuple.tuple(firstB.getId(), firstB.getImageUrl()));
            assertThat(result.get(second.id())).extracting(NoticeImageLink::imageId).containsExactly(secondA.getId());
            // linkId 는 실제 연결 행 ID 다(알림 응답 계약).
            assertThat(result.values().stream().flatMap(List::stream).map(NoticeImageLink::linkId)).allMatch(linkIds::contains);
            // 연결 id 를 넘겨도 그 연결의 공지 이미지가 나오지 않는다(이전 결함).
            assertThat(adapter.findImageLinks(linkIds.stream()
                    .filter(id -> !List.of(first.id(), second.id(), other.id()).contains(id)).toList()))
                    .doesNotContainKeys(first.id(), second.id(), other.id());
        }

        @Test
        @DisplayName("이미지 없는 공지·없는 공지 id·빈 목록은 빈 결과다")
        void it_returns_empty_for_missing_and_empty_ids() {
            Notice noImage = notice(place);
            em.flush();
            em.clear();

            assertThat(adapter.findImageLinks(List.of(noImage.id()))).isEmpty();
            assertThat(adapter.findImageLinks(List.of(Long.MAX_VALUE))).isEmpty();
            assertThat(adapter.findImageLinks(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("legacy 호출자 호환은")
    class Describe_legacy_callers {

        @Test
        @DisplayName("회원 삭제의 reassignMember JPQL 이 작성자를 dummy 로 바꾼다")
        void it_reassigns_author_to_dummy() {
            Notice notice = notice(place);
            MemberJpaEntity dummy = member("dummy", "01000000002");
            em.flush();

            noticeJpaRepository.reassignMember(author, dummy);

            assertThat(adapter.findById(notice.id()).orElseThrow().authorId()).isEqualTo(dummy.getId());
        }

        @Test
        @DisplayName("공개 command 용 reassignAuthor 가 해당 회원 작성 공지만 dummy 로 바꾼다")
        void it_reassigns_author_through_port() {
            Notice mine = notice(place);
            MemberJpaEntity other = member("other", "01000000003");
            Notice others = adapter.save(Notice.create("제목", "내용", place.getId(), other.getId()));
            MemberJpaEntity dummy = member("dummy2", "01000000004");
            em.flush();

            adapter.reassignAuthor(author.getId(), dummy.getId());

            assertThat(adapter.findById(mine.id()).orElseThrow().authorId()).isEqualTo(dummy.getId());
            assertThat(adapter.findById(others.id()).orElseThrow().authorId()).isEqualTo(other.getId());
        }

        @Test
        @DisplayName("이미지 정리용 unlinkImage 가 해당 연결만 지운다")
        void it_deletes_single_notice_image() {
            Notice notice = notice(place);
            ImageJpaEntity keep = image("keep");
            ImageJpaEntity drop = image("drop");
            adapter.addImages(notice.id(), List.of(keep.getId(), drop.getId()));
            em.flush();

            adapter.unlinkImage(notice.id(), drop.getId());
            em.flush();
            em.clear();

            assertThat(adapter.findImages(notice.id())).extracting(NoticeImageResult::imageId).containsExactly(keep.getId());
        }
    }

    @Nested
    @DisplayName("bridge adapter 는")
    class Describe_bridges {

        @Test
        @DisplayName("지점·본부·소속 회원·존재 이미지를 legacy 저장소에서 읽는다")
        void it_reads_from_legacy_repositories() {
            ImageJpaEntity image = image("exists");
            em.flush();
            em.clear();

            assertThat(placeAdapter.findPlace(place.getId())).contains(new NoticePlace(place.getId(), "광산구", head.getId()));
            assertThat(placeAdapter.findPlace(-1)).isEmpty();
            assertThat(placeAdapter.findPlaceIdsInSameHead(place.getId())).containsExactlyInAnyOrder(place.getId(), sibling.getId());
            assertThat(memberAdapter.findPlaceIdOf(author.getId())).contains(place.getId());
            assertThat(memberAdapter.findPlaceIdOf(-1L)).isEmpty();
            assertThat(memberAdapter.findMemberIdsInPlace(place.getId())).containsExactly(author.getId());
            assertThat(imageAdapter.findExistingImageIds(List.of(image.getId(), image.getId(), -1L))).containsExactly(image.getId());
        }

        @Test
        @DisplayName("본부가 없는 지점은 본부가 없는 지점끼리 같은 본부로 본다 (기존 findByHead(null) 의미)")
        void it_groups_places_without_head() {
            PlaceJpaEntity lonely = em.persist(PlaceJpaEntity.builder().name("본부 없음").build());
            PlaceJpaEntity lonely2 = em.persist(PlaceJpaEntity.builder().name("본부 없음2").build());
            em.flush();
            em.clear();

            assertThat(placeAdapter.findPlace(lonely.getId())).contains(new NoticePlace(lonely.getId(), "본부 없음", null));
            assertThat(placeAdapter.findPlaceIdsInSameHead(lonely.getId())).containsExactlyInAnyOrder(lonely.getId(), lonely2.getId());
        }
    }

    private Notice notice(PlaceJpaEntity target) {
        return adapter.save(Notice.create("제목", "내용", target.getId(), author.getId()));
    }

    private ImageJpaEntity image(String name) {
        return em.persist(ImageJpaEntity.builder().imageUrl("https://test.local/" + name).build());
    }

    private MemberJpaEntity member(String nickname, String phoneNumber) {
        return em.persist(MemberJpaEntity.builder()
                .name(nickname)
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phoneNumber)
                .role(MemberRole.ROLE_PLACE_ADMIN)
                .status(MemberStatus.ACTIVE)
                .build());
    }
}
