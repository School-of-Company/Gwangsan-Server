package team.startup.gwangsan.domain.relatedkeyword.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.relatedkeyword.presentation.dto.response.RelatedKeywordResponse;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.service.FindRelatedKeywordService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindRelatedKeywordServiceImpl implements FindRelatedKeywordService {

    private final RelatedKeywordRepository relatedKeywordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RelatedKeywordResponse> execute() {
        return relatedKeywordRepository.findAll().stream()
                .map(keyword -> new RelatedKeywordResponse(keyword.getId(), keyword.getName()))
                .collect(Collectors.toList());
    }
}
