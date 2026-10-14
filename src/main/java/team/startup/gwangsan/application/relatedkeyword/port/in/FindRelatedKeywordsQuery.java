package team.startup.gwangsan.application.relatedkeyword.port.in;

import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;

import java.util.List;

public interface FindRelatedKeywordsQuery {
    List<RelatedKeywordResult> query();
}
