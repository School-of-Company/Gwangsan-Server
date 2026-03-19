package team.startup.gwangsan.domain.report.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateMemberReportRequest;
import team.startup.gwangsan.domain.report.service.CreateMemberReportService;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final CreateMemberReportService createMemberReportService;

    @PostMapping
    public ResponseEntity<Void> createReport(@RequestBody @Valid CreateMemberReportRequest request) {
        createMemberReportService.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
