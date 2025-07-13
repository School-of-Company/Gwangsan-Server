package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.service.GetMyReviewListService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetMyReviewListServiceImpl implements GetMyReviewListService {

    private final ReviewRepository reviewRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> execute(String type, String mode) {
        Member reviewer = memberUtil.getCurrentMember();

        return reviewRepository.findAllByReviewer(reviewer).stream()
                .filter(review ->
                        review.getProduct().getType().name().equalsIgnoreCase(type) &&
                                review.getProduct().getMode().name().equalsIgnoreCase(mode))
                .map(review -> new ReviewResponse(
                        review.getProduct().getId(),
                        review.getContent(),
                        review.getLight(),
                        reviewer.getNickname()
                ))
                .toList();
    }
}
