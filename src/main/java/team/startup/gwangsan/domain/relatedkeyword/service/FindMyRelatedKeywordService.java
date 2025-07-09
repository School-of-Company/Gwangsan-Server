package team.startup.gwangsan.domain.relatedkeyword.service;

import team.startup.gwangsan.domain.relatedkeyword.presentation.dto.response.MyRelatedKeywordResponse;
import java.util.List;

public interface FindMyRelatedKeywordService {
    List<MyRelatedKeywordResponse> execute();
}
