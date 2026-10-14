package team.startup.gwangsan.application.notice.port.out;

import java.util.List;

public interface NoticeImagePort {

    // 존재하는 이미지 id. 중복 요청 id 는 한 번만 돌려준다.
    List<Long> findExistingImageIds(List<Long> imageIds);
}
