package team.startup.gwangsan.domain.block.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("MemberBlockCustomRepositoryImpl H2 슬라이스 통합 테스트")
class MemberBlockCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    private MemberBlockCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MemberBlockCustomRepositoryImpl(queryFactory);
    }

    @Nested
    @DisplayName("차단 관계 존재 조회는")
    class Describe_existsBlockBetween {

        @Test
        @DisplayName("저장 방향과 반대 방향 모두를 같은 차단 관계로 판단한다")
        void it_treats_a_persisted_block_as_existing_in_both_member_orders() {
            Member blocker = member("차단자");
            Member blocked = member("차단대상");
            Member unrelated = member("무관회원");
            em.persist(MemberBlock.builder().blocker(blocker).blocked(blocked).build());
            em.flush();
            em.clear();

            assertThat(repository.existsBlockBetween(blocker.getId(), blocked.getId())).isTrue();
            assertThat(repository.existsBlockBetween(blocked.getId(), blocker.getId())).isTrue();
            assertThat(repository.existsBlockBetween(blocker.getId(), unrelated.getId())).isFalse();
        }
    }

    private Member member(String suffix) {
        return em.persist(Member.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-1000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }
}
