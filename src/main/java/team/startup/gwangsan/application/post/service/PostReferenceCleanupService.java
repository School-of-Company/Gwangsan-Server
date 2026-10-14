package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.port.in.PostMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.post.port.in.UnlinkProductImageUseCase;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.application.post.port.out.ReservationPort;

/** 다른 업무의 삭제(회원 탈퇴·이미지 삭제)가 게시글 쪽 참조를 정리할 때 쓰는 command. */
@Service
@RequiredArgsConstructor
@Transactional
public class PostReferenceCleanupService implements PostMemberReferenceCleanupUseCase, UnlinkProductImageUseCase {

    private final ProductPort productPort;
    private final ReservationPort reservationPort;
    private final ProductImagePort productImagePort;

    @Override
    public void reassignAuthor(Long memberId, Long dummyMemberId) {
        productPort.reassignAuthor(memberId, dummyMemberId);
    }

    @Override
    public void reassignReserver(Long memberId, Long dummyMemberId) {
        reservationPort.reassignReserver(memberId, dummyMemberId);
    }

    @Override
    public void unlink(Long productId, Long imageId) {
        productImagePort.unlink(productId, imageId);
    }
}
