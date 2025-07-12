package team.startup.gwangsan.domain.report.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateProductReportRequest;
import team.startup.gwangsan.domain.report.service.CreateProductReportService;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final CreateProductReportService createProductReportService;

    @PostMapping
    public ResponseEntity<Void> createReport(@RequestBody @Valid CreateProductReportRequest request) {
        createProductReportService.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
