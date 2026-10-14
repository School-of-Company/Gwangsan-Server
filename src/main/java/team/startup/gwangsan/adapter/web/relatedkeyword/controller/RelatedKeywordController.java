package team.startup.gwangsan.adapter.web.relatedkeyword.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.adapter.web.relatedkeyword.dto.RelatedKeywordResponse;
import team.startup.gwangsan.application.relatedkeyword.data.DeleteMemberRelatedKeywordCommand;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteMemberRelatedKeywordUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.in.FindMemberRelatedKeywordsQuery;
import team.startup.gwangsan.application.relatedkeyword.port.in.FindRelatedKeywordsQuery;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

@RestController
@RequestMapping("/api/related-keyword")
@RequiredArgsConstructor
public class RelatedKeywordController {

    private final FindRelatedKeywordsQuery findRelatedKeywordsQuery;
    private final FindMemberRelatedKeywordsQuery findMemberRelatedKeywordsQuery;
    private final DeleteMemberRelatedKeywordUseCase deleteMemberRelatedKeywordUseCase;

    @GetMapping
    public ResponseEntity<List<RelatedKeywordResponse>> findAllKeywords() {
        return ResponseEntity.ok(findRelatedKeywordsQuery.query().stream()
                .map(RelatedKeywordResponse::from)
                .toList());
    }

    @GetMapping("/current")
    public ResponseEntity<List<RelatedKeywordResponse>> findMyKeywords(ActorContext actor) {
        return ResponseEntity.ok(findMemberRelatedKeywordsQuery.query(actor.memberId()).stream()
                .map(RelatedKeywordResponse::from)
                .toList());
    }

    @DeleteMapping("/{memberRelatedKeywordId}")
    public ResponseEntity<Void> deleteMyKeyword(ActorContext actor, @PathVariable("memberRelatedKeywordId") Long id) {
        deleteMemberRelatedKeywordUseCase.execute(new DeleteMemberRelatedKeywordCommand(actor.memberId(), id));
        return ResponseEntity.noContent().build();
    }
}
