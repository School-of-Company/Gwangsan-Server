package team.startup.gwangsan.domain.member.peresentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindMyInfoResponse;
import team.startup.gwangsan.domain.member.service.FindMyInfoService;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final FindMyInfoService getMyInfoService;

    @GetMapping
    public ResponseEntity<FindMyInfoResponse> findMyInfo() {
        return ResponseEntity.ok(getMyInfoService.execute());
    }
}

