package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.service.GetReceivedReviewListService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

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

        return reviewRepository.findAllByReviewed(reviewed).stream()
                .map(review -> {
                    List<GetImageResponse> images = productImageRepository.findAllByProduct(review.getProduct()).stream()
                            .map(pi -> new GetImageResponse(
                                    pi.getImage().getId(),
                                    pi.getImage().getImageUrl()
                            ))
                            .toList();

                    return new ReviewResponse(
                            review.getProduct().getId(),
                            review.getContent(),
                            review.getLight(),
                            review.getReviewer().getNickname(),
                            images
                    );
                })
                .toList();
    }
}
