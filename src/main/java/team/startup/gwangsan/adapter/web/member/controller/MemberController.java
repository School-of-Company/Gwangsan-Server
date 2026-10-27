package team.startup.gwangsan.adapter.web.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.member.dto.request.UpdateMyInfoRequest;
import team.startup.gwangsan.adapter.web.member.dto.request.UpdateMyPlaceRequest;
import team.startup.gwangsan.adapter.web.member.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.adapter.web.member.dto.response.FindMyInfoResponse;
import team.startup.gwangsan.adapter.web.member.dto.response.FindUserInfoResponse;
import team.startup.gwangsan.application.member.data.MyInfoResult;
import team.startup.gwangsan.application.member.data.UpdateMyInfoCommand;
import team.startup.gwangsan.application.member.data.UpdateMyPlaceCommand;
import team.startup.gwangsan.application.member.data.UserInfoResult;
import team.startup.gwangsan.application.member.port.in.FindAllUserInfoQuery;
import team.startup.gwangsan.application.member.port.in.FindMyInfoQuery;
import team.startup.gwangsan.application.member.port.in.FindUserInfoQuery;
import team.startup.gwangsan.application.member.port.in.UpdateMyInfoUseCase;
import team.startup.gwangsan.application.member.port.in.UpdateMyPlaceUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.application.member.data.WithdrawMemberCommand;
import team.startup.gwangsan.application.member.port.in.WithdrawMemberUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final FindMyInfoQuery findMyInfoQuery;
    private final UpdateMyInfoUseCase updateMyInfoUseCase;
    private final UpdateMyPlaceUseCase updateMyPlaceUseCase;
    private final FindUserInfoQuery findUserInfoQuery;
    private final FindAllUserInfoQuery findAllUserInfoQuery;
    // bridge: 탈퇴·회원 삭제는 모든 업무 참조를 정리하는 PR11 에서 member input Port 로 교체한다.
    private final WithdrawMemberUseCase withdrawMemberUseCase;

    @GetMapping
    public ResponseEntity<FindMyInfoResponse> findMyInfo(ActorContext actor) {
        MyInfoResult r = findMyInfoQuery.query(actor);
        // 기존 응답 계약 보존: dongName 자리에 본부 이름, headName 자리에 동 이름이 나간다.
        return ResponseEntity.ok(new FindMyInfoResponse(r.memberId(), r.nickname(), r.placeName(), r.headName(),
                r.dongName(), r.light(), r.gwangsan(), r.description(), r.specialties()));
    }

    @PatchMapping
    public ResponseEntity<Void> updateMyInfo(ActorContext actor, @RequestBody @Valid UpdateMyInfoRequest request) {
        updateMyInfoUseCase.execute(new UpdateMyInfoCommand(actor, request.nickname(), request.description(),
                request.specialties()));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/place")
    public ResponseEntity<Void> updateMyPlace(ActorContext actor, @RequestBody @Valid UpdateMyPlaceRequest request) {
        updateMyPlaceUseCase.execute(new UpdateMyPlaceCommand(actor, request.placeId()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FindUserInfoResponse> findUserInfo(@PathVariable("id") Long memberId) {
        UserInfoResult r = findUserInfoQuery.query(memberId);
        return ResponseEntity.ok(new FindUserInfoResponse(r.memberId(), r.nickname(), r.placeName(), r.light(),
                r.gwangsan(), r.description(), r.specialties(), r.name(), r.phoneNumber(), r.role(), r.status(),
                r.joinedAt()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FindAllUserInfoResponse>> findAllUserInfo(
            ActorContext actor,
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "placeName", required = false) String placeName
    ) {
        return ResponseEntity.ok(findAllUserInfoQuery.query(actor, nickname, placeName).stream()
                .map(p -> new FindAllUserInfoResponse(p.memberId(), p.nickname(), p.name(), p.phoneNumber(),
                        p.role(), p.status(), p.joinedAt(), p.gwangsan(), p.placeName()))
                .toList());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMember(ActorContext actor) {
        withdrawMemberUseCase.execute(new WithdrawMemberCommand(actor.memberId(), false));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
