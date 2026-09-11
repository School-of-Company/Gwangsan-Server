package team.startup.gwangsan.domain.report.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateReportRequest;
import team.startup.gwangsan.domain.report.service.CreateReportService;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final CreateReportService createReportService;

    @PostMapping
    public ResponseEntity<Void> createReport(@RequestBody @Valid CreateReportRequest request) {
        createReportService.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
