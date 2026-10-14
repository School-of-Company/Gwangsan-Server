package team.startup.gwangsan.application.report.port.out;

import team.startup.gwangsan.domain.report.model.Report;

import java.util.List;

public interface SaveReportPort {

    /** @return 저장된 신고 ID */
    Long save(Report report);

    void saveImages(Long reportId, List<Long> imageIds);

    void deleteImageLink(Long reportId, Long imageId);

    /** 신고자 → 피신고자 순서로 회원 참조를 bulk 재지정한다(영속성 컨텍스트 자동 clear). */
    void reassignMember(Long memberId, Long dummyMemberId);
}
