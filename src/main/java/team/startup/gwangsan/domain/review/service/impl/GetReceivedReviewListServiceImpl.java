package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.repository.projection.ReceivedReviewDto;
import team.startup.gwangsan.domain.review.service.GetReceivedReviewListService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetReceivedReviewListServiceImpl implements GetReceivedReviewListService {

    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> execute() {

        Member reviewed = memberUtil.getCurrentMember();

        List<ReceivedReviewDto> rows =
                reviewRepository.findReceivedReviews(reviewed.getId());

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
                        imagesByProductId.getOrDefault(r.productId(), List.of())
                ))
                .toList();
    }
}