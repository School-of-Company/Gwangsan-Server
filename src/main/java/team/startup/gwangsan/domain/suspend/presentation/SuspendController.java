package team.startup.gwangsan.domain.suspend.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.suspend.presentation.dto.request.SuspendMemberRequest;
import team.startup.gwangsan.domain.suspend.service.SuspendMemberService;

@RestController
@RequestMapping("/api/suspend")
@RequiredArgsConstructor
public class SuspendController {

    private final SuspendMemberService suspendMemberService;

    @PatchMapping
    public ResponseEntity<Void> suspend(@RequestBody @Valid SuspendMemberRequest request) {
        suspendMemberService.execute(request.memberId(), request.suspendedDays(), request.alertId());
        return ResponseEntity.noContent().build();
    }
}
