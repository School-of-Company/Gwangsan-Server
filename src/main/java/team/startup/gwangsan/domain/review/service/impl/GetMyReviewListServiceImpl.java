package team.startup.gwangsan.domain.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
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
    public List<ReviewResponse> execute(Type type, Mode mode) {
        Member reviewer = memberUtil.getCurrentMember();

        return reviewRepository.findAllByReviewerAndProduct_TypeAndProduct_Mode(reviewer, type, mode)
                .stream()
                .map(review -> new ReviewResponse(
                        review.getProduct().getId(),
                        review.getContent(),
                        review.getLight(),
                        reviewer.getNickname()
                ))
                .toList();
    }
}

