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
import team.startup.gwangsan.domain.review.service.GetReviewByMemberService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetReviewByMemberServiceImpl implements GetReviewByMemberService {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> execute(Long memberId) {
        Member reviewedMember = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        return reviewRepository.findAllByReviewed(reviewedMember).stream()
                .map(review -> {
                    List<GetImageResponse> images = productImageRepository.findAllByProductId(review.getProduct().getId()).stream()
                            .map(pi -> new GetImageResponse(
                                    pi.getImage().getId(),
                                    pi.getImage().getImageUrl()
                            ))
                            .toList();

                    return new ReviewResponse(
                            review.getId(),
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

