package team.startup.gwangsan.adapter.integration.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.report.port.out.LoadReportImagePort;

import java.util.List;

/** report → image(PR05) 공개 query 연결. 없는 이미지 ID 는 기존 findAllById 처럼 조용히 빠진다. */
@Component
@RequiredArgsConstructor
public class ReportImageBridgeAdapter implements LoadReportImagePort {

    private final ImageQuery imageQuery;

    @Override
    public List<Long> findExistingImageIds(List<Long> imageIds) {
        return imageQuery.findAllByIds(imageIds).stream().map(ImageSummary::imageId).toList();
    }
}
