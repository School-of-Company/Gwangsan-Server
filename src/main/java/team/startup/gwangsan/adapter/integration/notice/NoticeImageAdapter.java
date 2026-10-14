package team.startup.gwangsan.adapter.integration.notice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.notice.port.out.NoticeImagePort;

import java.util.List;

// image 공개 ImageQuery(PR05)로 연결한다. 중복 id 거절은 공지 서비스의 개수 비교가 맡으므로 getAllByIds 를 쓰지 않는다.
@Component
@RequiredArgsConstructor
public class NoticeImageAdapter implements NoticeImagePort {

    private final ImageQuery imageQuery;

    @Override
    public List<Long> findExistingImageIds(List<Long> imageIds) {
        return imageQuery.findAllByIds(imageIds).stream()
                .map(ImageSummary::imageId)
                .toList();
    }
}
