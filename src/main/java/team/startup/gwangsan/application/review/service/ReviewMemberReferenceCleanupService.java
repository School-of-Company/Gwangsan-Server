package team.startup.gwangsan.application.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.review.port.in.ReviewMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.review.port.out.ReviewPort;

/** 회원 탈퇴 시 후기의 회원 참조 재지정. 탈퇴 트랜잭션에 참여한다. */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewMemberReferenceCleanupService implements ReviewMemberReferenceCleanupUseCase {

    private final ReviewPort reviewPort;

    @Override
    public void reassign(Long memberId, Long dummyMemberId) {
        reviewPort.reassignReviewer(memberId, dummyMemberId);
        reviewPort.reassignReviewed(memberId, dummyMemberId);
    }
}
