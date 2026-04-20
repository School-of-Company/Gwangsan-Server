package team.startup.gwangsan.domain.member.peresentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.member.peresentation.dto.request.UpdateMyInfoRequest;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindMyInfoResponse;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindUserInfoResponse;
import team.startup.gwangsan.domain.member.service.*;
import team.startup.gwangsan.global.dto.response.SliceResponse;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final FindMyInfoService getMyInfoService;
    private final UpdateMyInfoService updateMyInfoService;
    private final FindUserInfoService findUserInfoService;
    private final FindAllUserInfoService findAllUserInfoService;
    private final MemberWithdrawalService memberWithdrawalService;

    @GetMapping
    public ResponseEntity<FindMyInfoResponse> findMyInfo() {
        return ResponseEntity.ok(getMyInfoService.execute());
    }

    @PatchMapping
    public ResponseEntity<Void> updateMyInfo(@RequestBody @Valid UpdateMyInfoRequest request) {
        updateMyInfoService.execute(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FindUserInfoResponse> findUserInfo(@PathVariable("id") Long memberId) {
        FindUserInfoResponse response = findUserInfoService.execute(memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<SliceResponse<FindAllUserInfoResponse>> findAllUserInfo(
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String placeName,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                SliceResponse.from(findAllUserInfoService.execute(nickname, placeName, pageable))
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMember() {
        memberWithdrawalService.execute();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
