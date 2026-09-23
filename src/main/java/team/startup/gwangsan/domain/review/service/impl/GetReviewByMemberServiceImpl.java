package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.repository.projection.ReceivedReviewDto;
import team.startup.gwangsan.domain.review.service.GetReviewByMemberService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetReviewByMemberServiceImpl implements GetReviewByMemberService {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> execute(Long memberId) {
        return findReviews(memberId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> execute(Long memberId, Long cursor, int size) {
        return findReviews(memberId, cursor, size);
    }

    private List<ReviewResponse> findReviews(Long memberId, Long cursor, Integer size) {

        Member target = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        List<ReceivedReviewDto> rows = size == null
                ? reviewRepository.findReceivedReviews(memberId)
                : reviewRepository.findReceivedReviews(memberId, cursor, size);
        if (rows.isEmpty()) return List.of();

        List<Long> productIds = rows.stream()
                .map(ReceivedReviewDto::productId)
                .distinct()
                .toList();

        Map<Long, List<GetImageResponse>> imagesByProductId =
                productImageRepository.findAllByProductIdIn(productIds).stream()
                        .collect(Collectors.groupingBy(
                                pi -> pi.getProduct().getId(),
                                Collectors.mapping(
                                        pi -> new GetImageResponse(
                                                pi.getImage().getId(),
                                                pi.getImage().getImageUrl()
                                        ),
                                        Collectors.toList()
                                )
                        ));

        return rows.stream()
                .map(r -> new ReviewResponse(
                        r.reviewId(),
                        r.productId(),
                        r.content(),
                        r.light(),
                        r.reviewerNickname(),
                        target.getNickname(),
                        imagesByProductId.getOrDefault(r.productId(), List.of())
                ))
                .toList();
    }
}