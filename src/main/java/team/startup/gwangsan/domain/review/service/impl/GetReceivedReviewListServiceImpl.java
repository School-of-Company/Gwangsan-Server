package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.service.GetReceivedReviewListService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetReceivedReviewListServiceImpl implements GetReceivedReviewListService {

    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "receivedReviews",
            key = "#root.target.getCurrentMemberId()"
    )
    public List<ReviewResponse> execute() {

        Member reviewed = memberUtil.getCurrentMember();

        List<Review> reviews =
                reviewRepository.findAllByReviewedWithFetch(reviewed);

        if (reviews.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = reviews.stream()
                .map(review -> review.getProduct().getId())
                .collect(Collectors.toSet());

        List<ProductImage> productImages =
                productImageRepository.findAllByProductIdIn(productIds);

        Map<Long, List<GetImageResponse>> imageMap = productImages.stream()
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

        return reviews.stream()
                .map(review -> {
                    Long productId = review.getProduct().getId();
                    List<GetImageResponse> images =
                            imageMap.getOrDefault(productId, List.of());

                    return new ReviewResponse(
                            review.getId(),
                            productId,
                            review.getContent(),
                            review.getLight(),
                            review.getReviewer().getNickname(),
                            images
                    );
                })
                .toList();
    }

    public Long getCurrentMemberId() {
        return memberUtil.getCurrentMember().getId();
    }
}