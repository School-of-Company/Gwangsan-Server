package team.startup.gwangsan.domain.review.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.repository.projection.MyReviewDto;
import team.startup.gwangsan.domain.review.repository.projection.ReceivedReviewDto;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("ReviewCustomRepositoryImpl H2 슬라이스 통합 테스트")
class ReviewCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    private ReviewCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ReviewCustomRepositoryImpl(queryFactory);
    }

    @Nested
    @DisplayName("내가 작성한 리뷰 조회는")
    class Describe_findMyReviews {

        @Test
        @DisplayName("리뷰 작성자를 고정하고 대상 별명과 ID 내림차순 투영을 반환한다")
        void it_returns_only_reviewer_reviews_with_reviewed_nickname_in_descending_id_order() {
            Member reviewer = member("작성자");
            Member firstReviewed = member("첫대상");
            Member secondReviewed = member("둘대상");
            Member otherReviewer = member("다른작성자");
            Review oldest = review(reviewer, firstReviewed, "첫 리뷰", 3);
            Review newest = review(reviewer, secondReviewed, "둘 리뷰", 7);
            review(otherReviewer, firstReviewed, "제외 리뷰", 9);
            em.flush();
            em.clear();

            List<MyReviewDto> result = repository.findMyReviews(reviewer.getId());

            assertThat(result).extracting(
                    MyReviewDto::reviewId,
                    MyReviewDto::productId,
                    MyReviewDto::content,
                    MyReviewDto::light,
                    MyReviewDto::reviewedNickname
            ).containsExactly(
                    org.assertj.core.groups.Tuple.tuple(newest.getId(), newest.getProduct().getId(), "둘 리뷰", 7, "회원-둘대상"),
                    org.assertj.core.groups.Tuple.tuple(oldest.getId(), oldest.getProduct().getId(), "첫 리뷰", 3, "회원-첫대상")
            );
        }
    }

    @Nested
    @DisplayName("내가 받은 리뷰 조회는")
    class Describe_findReceivedReviews {

        @Test
        @DisplayName("리뷰 대상을 고정하고 작성자 별명과 ID 내림차순 투영을 반환한다")
        void it_returns_only_received_reviews_with_reviewer_nickname_in_descending_id_order() {
            Member reviewed = member("대상");
            Member firstReviewer = member("첫작성자");
            Member secondReviewer = member("둘작성자");
            Member otherReviewed = member("다른대상");
            Review oldest = review(firstReviewer, reviewed, "첫 받은 리뷰", 2);
            Review newest = review(secondReviewer, reviewed, "둘 받은 리뷰", 8);
            review(firstReviewer, otherReviewed, "제외 받은 리뷰", 1);
            em.flush();
            em.clear();

            List<ReceivedReviewDto> result = repository.findReceivedReviews(reviewed.getId());

            assertThat(result).extracting(
                    ReceivedReviewDto::reviewId,
                    ReceivedReviewDto::productId,
                    ReceivedReviewDto::content,
                    ReceivedReviewDto::light,
                    ReceivedReviewDto::reviewerNickname
            ).containsExactly(
                    org.assertj.core.groups.Tuple.tuple(newest.getId(), newest.getProduct().getId(), "둘 받은 리뷰", 8, "회원-둘작성자"),
                    org.assertj.core.groups.Tuple.tuple(oldest.getId(), oldest.getProduct().getId(), "첫 받은 리뷰", 2, "회원-첫작성자")
            );
        }
    }

    private Member member(String suffix) {
        return em.persist(Member.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-0000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Review review(Member reviewer, Member reviewed, String content, int light) {
        Product product = em.persist(Product.builder()
                .title("상품-" + content)
                .description("설명")
                .gwangsan(5000)
                .member(reviewer)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .status(ProductStatus.ONGOING)
                .build());
        return em.persist(Review.builder()
                .reviewer(reviewer)
                .reviewed(reviewed)
                .product(product)
                .content(content)
                .light(light)
                .build());
    }
}
