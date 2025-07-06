package team.startup.gwangsan.domain.notice.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.CreateNoticeRequest;
import team.startup.gwangsan.domain.notice.presentation.dto.reqeust.UpdateNoticeRequest;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindAllNoticeResponse;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindNoticeResponse;
import team.startup.gwangsan.domain.notice.service.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NoticeController {

    private final CreateNoticeService createNoticeService;
    private final FindAllNoticeService findAllNoticeService;
    private final FindNoticeService findNoticeService;
    private final UpdateNoticeService updateNoticeService;
    private final DeleteNoticeService deleteNoticeService;

    @PostMapping
    public ResponseEntity<Void> createNotice(@RequestBody @Valid CreateNoticeRequest request) {
        createNoticeService.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<FindAllNoticeResponse>> findAllNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<FindAllNoticeResponse> notices = findAllNoticeService.execute(page, size);
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<FindNoticeResponse> findNotice(
            @PathVariable Long noticeId
    ) {
        FindNoticeResponse response = findNoticeService.execute(noticeId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{noticeId}")
    public ResponseEntity<Void> updateNotice(
            @PathVariable("noticeId") Long noticeId,
            @RequestBody @Valid UpdateNoticeRequest request
    ) {
        updateNoticeService.execute(noticeId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        deleteNoticeService.execute(noticeId);
        return ResponseEntity.noContent().build();
    }
}
