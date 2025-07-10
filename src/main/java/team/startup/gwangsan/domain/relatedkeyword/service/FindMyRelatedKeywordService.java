package team.startup.gwangsan.domain.relatedkeyword.service;

import team.startup.gwangsan.domain.relatedkeyword.presentation.dto.response.RelatedKeywordResponse;

import java.util.List;

public interface FindMyRelatedKeywordService {
    List<RelatedKeywordResponse> execute();
}
