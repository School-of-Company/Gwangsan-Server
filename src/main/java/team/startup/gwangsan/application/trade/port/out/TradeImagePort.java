package team.startup.gwangsan.application.trade.port.out;

import java.util.List;

/** 이미지(image 소유) 존재 확인. */
public interface TradeImagePort {
    /** 존재하는 이미지 ID 만 돌려준다(없는 ID 는 빠짐). */
    List<Long> findExistingImageIds(List<Long> imageIds);
}
