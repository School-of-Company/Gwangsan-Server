package team.startup.gwangsan.application.report.port.in;

/** 신고와 이미지의 연결 행만 지운다(이미지 자체 삭제는 image 업무). */
public interface UnlinkReportImageUseCase {
    void unlink(Long reportId, Long imageId);
}
