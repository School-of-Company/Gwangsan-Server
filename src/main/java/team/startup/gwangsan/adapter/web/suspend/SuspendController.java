package team.startup.gwangsan.adapter.web.suspend;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.adapter.web.suspend.dto.SuspendMemberRequest;
import team.startup.gwangsan.application.suspend.data.SuspendMemberCommand;
import team.startup.gwangsan.application.suspend.port.in.SuspendMemberUseCase;

@RestController
@RequestMapping("/api/suspend")
@RequiredArgsConstructor
public class SuspendController {

    private final SuspendMemberUseCase suspendMemberUseCase;

    @PatchMapping
    public ResponseEntity<Void> suspend(@RequestBody @Valid SuspendMemberRequest request) {
        suspendMemberUseCase.execute(new SuspendMemberCommand(request.memberId(), request.suspendedDays(), request.alertId()));
        return ResponseEntity.noContent().build();
    }
}
