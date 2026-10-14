package team.startup.gwangsan.adapter.web.notice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.notice.dto.CreateNoticeRequest;
import team.startup.gwangsan.adapter.web.notice.dto.FindAllNoticeResponse;
import team.startup.gwangsan.adapter.web.notice.dto.FindNoticeResponse;
import team.startup.gwangsan.adapter.web.notice.dto.UpdateNoticeRequest;
import team.startup.gwangsan.application.notice.data.CreateNoticeCommand;
import team.startup.gwangsan.application.notice.data.DeleteNoticeCommand;
import team.startup.gwangsan.application.notice.data.NoticeDetailCriteria;
import team.startup.gwangsan.application.notice.data.NoticeDetailResult;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.application.notice.data.NoticeListCriteria;
import team.startup.gwangsan.application.notice.data.UpdateNoticeCommand;
import team.startup.gwangsan.application.notice.port.in.CreateNoticeUseCase;
import team.startup.gwangsan.application.notice.port.in.DeleteNoticeUseCase;
import team.startup.gwangsan.application.notice.port.in.NoticeDetailQuery;
import team.startup.gwangsan.application.notice.port.in.NoticeListQuery;
import team.startup.gwangsan.application.notice.port.in.UpdateNoticeUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NoticeController {

    private final CreateNoticeUseCase createNoticeUseCase;
    private final NoticeListQuery noticeListQuery;
    private final NoticeDetailQuery noticeDetailQuery;
    private final UpdateNoticeUseCase updateNoticeUseCase;
    private final DeleteNoticeUseCase deleteNoticeUseCase;

    @PostMapping
    public ResponseEntity<Void> createNotice(@RequestBody @Valid CreateNoticeRequest request, ActorContext actor) {
        createNoticeUseCase.execute(new CreateNoticeCommand(
                actor, request.title(), request.content(), request.placeId(), request.imageIds()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<FindAllNoticeResponse>> findAllNotices(
            ActorContext actor,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<FindAllNoticeResponse> notices = noticeListQuery.query(new NoticeListCriteria(actor, lastId, size)).stream()
                .map(n -> new FindAllNoticeResponse(n.id(), n.title(), n.content(), images(n.images()), n.isMe()))
                .toList();
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<FindNoticeResponse> findNotice(ActorContext actor, @PathVariable Long noticeId) {
        NoticeDetailResult n = noticeDetailQuery.query(new NoticeDetailCriteria(actor, noticeId));
        return ResponseEntity.ok(new FindNoticeResponse(
                n.id(), n.title(), n.content(), n.placeName(), n.createdAt(), n.viewerRole(), images(n.images()), n.isMe()));
    }

    @PatchMapping("/{noticeId}")
    public ResponseEntity<Void> updateNotice(
            @PathVariable("noticeId") Long noticeId,
            @RequestBody @Valid UpdateNoticeRequest request,
            ActorContext actor
    ) {
        updateNoticeUseCase.execute(new UpdateNoticeCommand(
                actor, noticeId, request.title(), request.content(), request.imageIds()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(ActorContext actor, @PathVariable Long noticeId) {
        deleteNoticeUseCase.execute(new DeleteNoticeCommand(actor, noticeId));
        return ResponseEntity.noContent().build();
    }

    private static List<GetImageResponse> images(List<NoticeImageResult> images) {
        return images.stream().map(i -> new GetImageResponse(i.imageId(), i.imageUrl())).toList();
    }
}
