package team.startup.gwangsan.adapter.integration.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.port.out.ChatImagePort;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** chat → image 공개 query. 하나라도 없으면 ImageNotFoundException(기존 오류)이 그대로 나간다. */
@Component
@RequiredArgsConstructor
public class ChatImageBridge implements ChatImagePort {

    private final ImageQuery imageQuery;

    @Override
    public Map<Long, String> requireImageUrls(List<Long> imageIds) {
        return imageQuery.getAllByIds(imageIds).stream()
                .collect(Collectors.toMap(ImageSummary::imageId, ImageSummary::imageUrl, (first, second) -> first));
    }
}
