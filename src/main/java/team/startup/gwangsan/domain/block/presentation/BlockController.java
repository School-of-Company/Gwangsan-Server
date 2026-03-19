package team.startup.gwangsan.domain.block.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.block.presentation.dto.response.GetBlockedMemberResponse;
import team.startup.gwangsan.domain.block.service.BlockMemberService;
import team.startup.gwangsan.domain.block.service.GetBlockListService;
import team.startup.gwangsan.domain.block.service.UnblockMemberService;

import java.util.List;

@RestController
@RequestMapping("/api/block")
@RequiredArgsConstructor
public class BlockController {

    private final BlockMemberService blockMemberService;
    private final UnblockMemberService unblockMemberService;
    private final GetBlockListService getBlockListService;

    @PostMapping("/{targetMemberId}")
    public ResponseEntity<Void> block(@PathVariable Long targetMemberId) {
        blockMemberService.execute(targetMemberId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<Void> unblock(@PathVariable Long targetMemberId) {
        unblockMemberService.execute(targetMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<GetBlockedMemberResponse>> getBlockList() {
        return ResponseEntity.ok(getBlockListService.execute());
    }
}
