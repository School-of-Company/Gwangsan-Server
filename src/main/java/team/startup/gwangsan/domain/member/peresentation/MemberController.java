package team.startup.gwangsan.domain.member.peresentation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.member.peresentation.dto.response.GetMyInfoResponse;
import team.startup.gwangsan.domain.member.service.GetMyInfoService;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final GetMyInfoService getMyInfoService;

    @GetMapping
    public ResponseEntity<GetMyInfoResponse> getMyInfo(HttpServletRequest request) {
        GetMyInfoResponse response = getMyInfoService.execute(request);
        return ResponseEntity.ok(response);
    }
}

