package team.startup.gwangsan.application.notice.service;

import team.startup.gwangsan.application.notice.port.out.NoticeImagePort;
import team.startup.gwangsan.domain.notice.exception.NoticeImageNotFoundException;

import java.util.List;

final class NoticeImages {

    private NoticeImages() {
    }

    // 요청 id 가 모두 존재해야 한다. 중복 id 도 개수 불일치로 거절한다(기존 동작).
    // 저장 순서는 조회 결과 순서를 따른다(기존 상세 이미지 순서 보존).
    static List<Long> existing(NoticeImagePort port, List<Long> imageIds) {
        List<Long> existing = port.findExistingImageIds(imageIds);
        if (existing.size() != imageIds.size()) {
            throw new NoticeImageNotFoundException();
        }
        return existing;
    }
}
