package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.trade.port.out.TradeImagePort;

import java.util.List;

/** trade → image 공개 query 연결. 기존 findAllById 와 같은 1 쿼리, 존재하는 이미지만. */
@Component
@RequiredArgsConstructor
public class TradeImageAdapter implements TradeImagePort {

    private final ImageQuery imageQuery;

    @Override
    public List<Long> findExistingImageIds(List<Long> imageIds) {
        return imageQuery.findAllByIds(imageIds).stream().map(ImageSummary::imageId).toList();
    }
}
