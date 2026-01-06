package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.repository.custom.MyReviewRow;
import team.startup.gwangsan.domain.review.service.GetMyReviewListService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetMyReviewListServiceImpl implements GetMyReviewListService {

    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> execute() {
        Member reviewer = memberUtil.getCurrentMember();

        List<MyReviewRow> rows = reviewRepository.findMyReviews(reviewer.getId());
        if (rows.isEmpty()) return List.of();

        List<Long> productIds = rows.stream()
                .map(MyReviewRow::productId)
                .distinct()
                .toList();

        Map<Long, List<GetImageResponse>> imagesByProductId =
                productImageRepository.findAllByProduct_IdIn(productIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                pi -> pi.getProduct().getId(),
                                java.util.stream.Collectors.mapping(
                                        pi -> new GetImageResponse(
                                                pi.getImage().getId(),
                                                pi.getImage().getImageUrl()
                                        ),
                                        java.util.stream.Collectors.toList()
                                )
                        ));

        return rows.stream()
                .map(r -> new ReviewResponse(
                        r.reviewId(),
                        r.productId(),
                        r.content(),
                        r.light(),
                        reviewer.getNickname(),
                        imagesByProductId.getOrDefault(r.productId(), List.of())
                ))
                .toList();
    }
}
