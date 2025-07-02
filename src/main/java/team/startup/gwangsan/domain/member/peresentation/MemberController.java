package team.startup.gwangsan.domain.member.peresentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.member.peresentation.dto.request.UpdateMyInfoRequest;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindMyInfoResponse;
import team.startup.gwangsan.domain.member.service.FindMyInfoService;
import team.startup.gwangsan.domain.member.service.UpdateMyInfoService;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final FindMyInfoService getMyInfoService;
    private final UpdateMyInfoService updateMyInfoService;

    @GetMapping
    public ResponseEntity<FindMyInfoResponse> findMyInfo() {
        return ResponseEntity.ok(getMyInfoService.execute());
    }

    @PatchMapping
    public ResponseEntity<Void> updateMyInfo(@RequestBody @Valid UpdateMyInfoRequest request) {
        updateMyInfoService.execute(request);
        return ResponseEntity.ok().build();
    }
}

