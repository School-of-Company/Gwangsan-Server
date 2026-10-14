package team.startup.gwangsan.application.image.port.in;

import team.startup.gwangsan.application.image.data.ImageSummary;

import java.util.Collection;
import java.util.List;

/**
 * 타 업무가 이미지 존재·URL 을 확인하는 공개 조회 계약.
 */
public interface ImageQuery {

    /** 존재하는 이미지만 반환한다. 없는 ID 는 조용히 빠진다(순서 보장 없음). */
    List<ImageSummary> findAllByIds(Collection<Long> imageIds);

    /** 요청 ID 가 모두 존재해야 한다(중복 ID 허용). 하나라도 없으면 ImageNotFoundException. 기존 ImageValidateUtil 의미. */
    List<ImageSummary> getAllByIds(List<Long> imageIds);
}
