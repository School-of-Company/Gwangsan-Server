package team.startup.gwangsan.application.feedback.port.in;

/** 문의와 이미지의 연결 행만 지운다(이미지 자체 삭제는 image 업무). */
public interface UnlinkFeedbackImageUseCase {
    void unlink(Long feedbackId, Long imageId);
}
