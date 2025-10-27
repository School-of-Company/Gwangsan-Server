package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewDetailResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.service.GetReviewDetailService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetReviewDetailServiceImpl implements GetReviewDetailService {

    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewDetailResponse execute(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(NotFoundReviewException::new);

        List<GetImageResponse> images = productImageRepository.findAllByProductId(review.getProduct().getId()).stream()
                .map(pi -> new GetImageResponse(
                        pi.getImage().getId(),
                        pi.getImage().getImageUrl()
                ))
                .toList();

        return new ReviewDetailResponse(
                review.getId(),
                review.getProduct().getTitle(),
                review.getContent(),
                review.getLight(),
                images
        );
    }
}
