package team.startup.gwangsan.adapter.integration.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackImagePort;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;

import java.util.List;

/** feedback → image 공개 query 연결. 없는 이미지 ID 는 기존 findAllById 처럼 조용히 빠진다. */
@Component
@RequiredArgsConstructor
public class FeedbackImageBridgeAdapter implements LoadFeedbackImagePort {

    private final ImageQuery imageQuery;

    @Override
    public List<Long> findExistingImageIds(List<Long> imageIds) {
        return imageQuery.findAllByIds(imageIds).stream().map(ImageSummary::imageId).toList();
    }
}
