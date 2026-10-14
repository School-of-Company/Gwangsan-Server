package team.startup.gwangsan.application.chat.port.out;

import java.util.List;
import java.util.Map;

public interface ChatImagePort {
    /** 이미지 ID → URL. 하나라도 없으면 이미지 부재 오류(404). */
    Map<Long, String> requireImageUrls(List<Long> imageIds);
}
