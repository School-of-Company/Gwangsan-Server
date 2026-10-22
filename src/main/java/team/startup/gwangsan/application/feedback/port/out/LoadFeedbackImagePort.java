package team.startup.gwangsan.application.feedback.port.out;

import java.util.List;

public interface LoadFeedbackImagePort {
    /** 존재하는 이미지 ID 만 돌려준다. 없는 ID 는 조용히 제외한다. */
    List<Long> findExistingImageIds(List<Long> imageIds);
}
