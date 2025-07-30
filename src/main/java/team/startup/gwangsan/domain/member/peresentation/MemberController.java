package team.startup.gwangsan.domain.member.peresentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.admin.service.WithDrawnMemberService;
import team.startup.gwangsan.domain.member.peresentation.dto.request.UpdateMyInfoRequest;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindMyInfoResponse;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindUserInfoResponse;
import team.startup.gwangsan.domain.member.service.*;

import java.util.List;

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
    public ResponseEntity<List<FindAllUserInfoResponse>> findAllUserInfo() {
        List<FindAllUserInfoResponse> response = findAllUserInfoService.execute();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMember() {
        memberWithdrawalService.execute();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

