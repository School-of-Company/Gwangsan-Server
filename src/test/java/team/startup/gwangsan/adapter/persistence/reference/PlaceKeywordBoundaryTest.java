package team.startup.gwangsan.adapter.persistence.reference;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.persistence.dong.DongPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.place.PlacePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.RelatedKeywordPersistenceAdapter;
import team.startup.gwangsan.application.dong.data.DongResult;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.model.MemberKeyword;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** PR03 persistence adapter 가 기존 테이블에서 순수 결과만 돌려주는지 실제 MariaDB 로 확인한다. */
@DataJpaTest
@Import({QueryDslConfig.class, PlacePersistenceAdapter.class, DongPersistenceAdapter.class, RelatedKeywordPersistenceAdapter.class})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("지점·동·관심 키워드 persistence 경계")
class PlaceKeywordBoundaryTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired EntityManager em;
    @Autowired PlacePersistenceAdapter places;
    @Autowired DongPersistenceAdapter dongs;
    @Autowired RelatedKeywordPersistenceAdapter keywords;

    private Head head;
    private Head otherHead;
    private Place first;
    private Place second;
    private Place otherBranch;
    private Place headless;

    @BeforeEach
    void seed() {
        head = persist(new Head("본점"));
        otherHead = persist(new Head("다른본점"));
        first = persist(new Place("첫지점", head));
        second = persist(new Place("둘째지점", head));
        otherBranch = persist(new Place("다른지점", otherHead));
        headless = persist(new Place("본점없음", null));
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("PlacePersistenceAdapter 는")
    class Describe_place {
        @Test
        void it_returns_place_snapshot_with_head_id() {
            assertThat(places.findById(first.getId())).contains(new PlaceResult(first.getId(), "첫지점", head.getId()));
            assertThat(places.findById(headless.getId())).contains(new PlaceResult(headless.getId(), "본점없음", null));
        }

        @Test
        void it_returns_empty_for_missing_place() {
            assertThat(places.findById(-1)).isEmpty();
        }

        @Test
        void it_returns_only_places_of_head() {
            assertThat(places.findAllByHeadId(head.getId())).extracting(PlaceResult::id)
                    .containsExactlyInAnyOrder(first.getId(), second.getId());
            assertThat(places.findAllByHeadId(-1)).isEmpty();
        }

        @Test
        void it_returns_requested_ids_only() {
            assertThat(places.findAllByIds(List.of(second.getId(), otherBranch.getId(), -1))).extracting(PlaceResult::name)
                    .containsExactlyInAnyOrder("둘째지점", "다른지점");
        }
    }

    @Nested
    @DisplayName("DongPersistenceAdapter 는")
    class Describe_dong {
        @Test
        void it_finds_dong_by_exact_name() {
            Dong dong = persist(new Dong("우산동"));
            em.flush();

            assertThat(dongs.findByName("우산동")).contains(new DongResult(dong.getId(), "우산동"));
            assertThat(dongs.findByName("없는동")).isEmpty();
        }
    }

    @Nested
    @DisplayName("RelatedKeywordPersistenceAdapter 는")
    class Describe_keyword {
        @Test
        void it_reads_keywords_and_member_mapping_and_deletes_only_target() {
            MemberJpaEntity owner = persist(member("010-1111-1111", "owner"));
            MemberJpaEntity other = persist(member("010-2222-2222", "other"));
            RelatedKeyword cooking = persist(new RelatedKeyword("요리"));
            RelatedKeyword cleaning = persist(new RelatedKeyword("청소"));
            MemberRelatedKeyword ownerCooking = persist(new MemberRelatedKeyword(cooking, owner));
            MemberRelatedKeyword otherCleaning = persist(new MemberRelatedKeyword(cleaning, other));
            em.flush();
            em.clear();

            assertThat(keywords.findAll()).containsExactlyInAnyOrder(
                    new RelatedKeywordResult(cooking.getId(), "요리"), new RelatedKeywordResult(cleaning.getId(), "청소"));
            // relatedKeywordId 는 매핑 ID 가 아니라 키워드 ID (기존 /current 계약)
            assertThat(keywords.findAllByMemberId(owner.getId()))
                    .containsExactly(new RelatedKeywordResult(cooking.getId(), "요리"));
            assertThat(keywords.findMemberKeyword(ownerCooking.getId()))
                    .contains(new MemberKeyword(ownerCooking.getId(), owner.getId()));
            assertThat(keywords.findMemberKeyword(-1L)).isEmpty();

            keywords.deleteMemberKeyword(ownerCooking.getId());
            em.flush();
            em.clear();

            assertThat(keywords.findMemberKeyword(ownerCooking.getId())).isEmpty();
            assertThat(keywords.findMemberKeyword(otherCleaning.getId())).isPresent();
            assertThat(keywords.findAll()).hasSize(2);
        }

        @Test
        void it_bulk_deletes_only_target_member_mappings_and_clears_context() {
            MemberJpaEntity owner = persist(member("010-1111-1111", "owner"));
            MemberJpaEntity other = persist(member("010-2222-2222", "other"));
            RelatedKeyword cooking = persist(new RelatedKeyword("요리"));
            persist(new MemberRelatedKeyword(cooking, owner));
            MemberRelatedKeyword otherCooking = persist(new MemberRelatedKeyword(cooking, other));
            em.flush();

            keywords.bulkDeleteAllByMemberId(owner.getId());

            // clearAutomatically: flush 없이도 즉시 반영되고 컨텍스트가 비워진다
            assertThat(em.contains(owner)).isFalse();
            assertThat(keywords.findAllByMemberId(owner.getId())).isEmpty();
            assertThat(keywords.findMemberKeyword(otherCooking.getId())).isPresent();
            assertThat(keywords.findAll()).hasSize(1);
        }
    }

    private MemberJpaEntity member(String phone, String nickname) {
        return MemberJpaEntity.builder().name(nickname).nickname(nickname).phoneNumber(phone).password("encoded")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
