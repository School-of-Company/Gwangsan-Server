package team.startup.gwangsan.domain.relatedkeyword.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.relatedkeyword.presentation.dto.response.RelatedKeywordResponse;
import team.startup.gwangsan.domain.relatedkeyword.service.DeleteMemberRelatedKeywordService;
import team.startup.gwangsan.domain.relatedkeyword.service.FindMyRelatedKeywordService;
import team.startup.gwangsan.domain.relatedkeyword.service.FindRelatedKeywordService;

import java.util.List;

@RestController
@RequestMapping("/api/related-keyword")
@RequiredArgsConstructor
public class RelatedKeywordController {

    private final FindRelatedKeywordService findRelatedKeywordService;
    private final FindMyRelatedKeywordService findMyRelatedKeywordService;
    private final DeleteMemberRelatedKeywordService deleteMemberRelatedKeywordService;

    @GetMapping
    public ResponseEntity<List<RelatedKeywordResponse>> findAllKeywords() {
        List<RelatedKeywordResponse> response = findRelatedKeywordService.execute();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    public ResponseEntity<List<RelatedKeywordResponse>> findMyKeywords() {
        List<RelatedKeywordResponse> response = findMyRelatedKeywordService.execute();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberRelatedKeywordId}")
    public ResponseEntity<Void> deleteMyKeyword(@PathVariable("memberRelatedKeywordId") Long id) {
        deleteMemberRelatedKeywordService.execute(id);
        return ResponseEntity.noContent().build();
    }
}
