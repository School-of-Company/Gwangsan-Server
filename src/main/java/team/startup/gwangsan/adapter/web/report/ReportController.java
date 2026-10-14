package team.startup.gwangsan.adapter.web.report;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.adapter.web.report.dto.CreateReportRequest;
import team.startup.gwangsan.application.report.port.in.CreateReportUseCase;
import team.startup.gwangsan.domain.common.ActorContext;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final CreateReportUseCase createReportUseCase;

    @PostMapping
    public ResponseEntity<Void> createReport(@RequestBody @Valid CreateReportRequest request, ActorContext actor) {
        createReportUseCase.execute(request.toCommand(actor.memberId()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
