package team.startup.gwangsan.application.report.port.out;

import java.util.List;

public interface LoadReportImagePort {

    /** 존재하는 이미지 ID 만 돌려준다. 없는 ID 는 조용히 제외한다(기존 findAllById 동작). */
    List<Long> findExistingImageIds(List<Long> imageIds);
}
