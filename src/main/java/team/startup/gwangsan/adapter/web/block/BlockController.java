package team.startup.gwangsan.adapter.web.block;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.adapter.web.block.dto.GetBlockedMemberResponse;
import team.startup.gwangsan.application.block.data.MemberBlockCommand;
import team.startup.gwangsan.application.block.port.in.BlockListQuery;
import team.startup.gwangsan.application.block.port.in.BlockMemberUseCase;
import team.startup.gwangsan.application.block.port.in.UnblockMemberUseCase;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

@RestController
@RequestMapping("/api/block")
@RequiredArgsConstructor
public class BlockController {

    private final BlockMemberUseCase blockMemberUseCase;
    private final UnblockMemberUseCase unblockMemberUseCase;
    private final BlockListQuery blockListQuery;

    @PostMapping("/{targetMemberId}")
    public ResponseEntity<Void> block(@PathVariable Long targetMemberId, ActorContext actor) {
        blockMemberUseCase.execute(new MemberBlockCommand(actor.memberId(), targetMemberId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<Void> unblock(@PathVariable Long targetMemberId, ActorContext actor) {
        unblockMemberUseCase.execute(new MemberBlockCommand(actor.memberId(), targetMemberId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<GetBlockedMemberResponse>> getBlockList(ActorContext actor) {
        return ResponseEntity.ok(blockListQuery.query(actor.memberId()).stream()
                .map(GetBlockedMemberResponse::from)
                .toList());
    }
}
