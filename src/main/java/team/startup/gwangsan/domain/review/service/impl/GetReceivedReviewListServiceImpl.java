package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.review.service.GetReceivedReviewListService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetReceivedReviewListServiceImpl implements GetReceivedReviewListService {

    private final ReviewRepository reviewRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> execute() {
        Member reviewed = memberUtil.getCurrentMember();

        return reviewRepository.findAllByReviewed(reviewed).stream()
                .map(review -> new ReviewResponse(
                        review.getProduct().getId(),
                        review.getContent(),
                        review.getLight(),
                        review.getReviewer().getNickname()
                ))
                .toList();
    }
}
