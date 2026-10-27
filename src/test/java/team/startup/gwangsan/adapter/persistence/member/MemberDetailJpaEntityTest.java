package team.startup.gwangsan.adapter.persistence.member;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("MemberDetailJpaEntity")
class MemberDetailJpaEntityTest {

    @Autowired private EntityManager em;

    private Long persistDetail(int gwangsan) {
        HeadJpaEntity head = new HeadJpaEntity("본부");
        em.persist(head);
        PlaceJpaEntity place = PlaceJpaEntity.builder().name("지점").head(head).build();
        em.persist(place);
        DongJpaEntity dong = DongJpaEntity.builder().name("동").build();
        em.persist(dong);
        MemberJpaEntity member = MemberJpaEntity.builder().name("회원").nickname("회원").phoneNumber("010")
                .password("pw").role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
        em.persist(member);
        em.persist(MemberDetailJpaEntity.builder().member(member).dong(dong).place(place)
                .gwangsan(gwangsan).light(0).description("소개").build());
        em.flush();
        return member.getId();
    }

    private MemberDetailJpaEntity memberDetail(Integer gwangsan, Integer light) {
        return MemberDetailJpaEntity.builder().gwangsan(gwangsan).light(light).build();
    }

    @Nested
    @DisplayName("원자적 UPDATE 뒤 같은 트랜잭션에서 다른 필드를 바꾸면")
    class Describe_write_back_after_atomic_update {

        @Test
        @DisplayName("영속성 컨텍스트의 옛 gwangsan 을 되쓰지 않는다")
        void it_does_not_write_back_stale_gwangsan() {
            Long memberId = persistDetail(1_000);
            em.clear();

            MemberDetailJpaEntity detail = em.find(MemberDetailJpaEntity.class, memberId);
            em.createQuery("UPDATE MemberDetail d SET d.gwangsan = d.gwangsan + 500 WHERE d.id = :id")
                    .setParameter("id", memberId)
                    .executeUpdate();
            detail.updateDescription("바뀐 소개");
            em.flush();
            em.clear();

            MemberDetailJpaEntity reloaded = em.find(MemberDetailJpaEntity.class, memberId);
            assertThat(reloaded.getGwangsan()).isEqualTo(1_500);
            assertThat(reloaded.getDescription()).isEqualTo("바뀐 소개");
        }
    }

    @Nested
    @DisplayName("plusLight() 메서드는")
    class Describe_plusLight {

        @Test
        @DisplayName("하한 0을 유지한다")
        void it_clamps_below_zero() {
            MemberDetailJpaEntity detail = memberDetail(0, 0);

            detail.plusLight(-1);

            assertThat(detail.getLight()).isZero();
        }

        @Test
        @DisplayName("상한 100을 유지한다")
        void it_clamps_above_one_hundred() {
            MemberDetailJpaEntity detail = memberDetail(0, 100);

            detail.plusLight(1);

            assertThat(detail.getLight()).isEqualTo(100);
        }

        @Test
        @DisplayName("범위 안의 점수는 그대로 더한다")
        void it_adds_light_within_boundaries() {
            MemberDetailJpaEntity detail = memberDetail(0, 50);

            detail.plusLight(10);

            assertThat(detail.getLight()).isEqualTo(60);
        }

        @Test
        @DisplayName("기존 점수와 증감값이 null이면 0으로 처리한다")
        void it_treats_null_light_and_delta_as_zero() {
            MemberDetailJpaEntity nullLight = memberDetail(0, null);
            MemberDetailJpaEntity existingLight = memberDetail(0, 50);

            nullLight.plusLight(null);
            existingLight.plusLight(null);

            assertThat(nullLight.getLight()).isZero();
            assertThat(existingLight.getLight()).isEqualTo(50);
        }
    }
}
