package team.startup.gwangsan.adapter.integration.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.report.port.out.LoadReportTargetPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

/** report → member(PR02)·post(PR06) 공개 query 연결. 삭제된 게시글은 기존과 같이 NotFoundProductException. */
@Component
@RequiredArgsConstructor
public class ReportTargetBridgeAdapter implements LoadReportTargetPort {

    private final MemberAccountQuery memberAccountQuery;
    private final ProductSnapshotQuery productSnapshotQuery;

    @Override
    public Long loadActiveProductOwnerId(Long productId) {
        return productSnapshotQuery.getActive(productId).authorId();
    }

    @Override
    public void checkMemberExists(Long memberId) {
        memberAccountQuery.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }
}
