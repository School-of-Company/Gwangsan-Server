package team.startup.gwangsan.domain.alert.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.alert.presentation.dto.response.GetAlertResponse;
import team.startup.gwangsan.domain.alert.service.FindAlertByCurrentService;

import java.util.List;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final FindAlertByCurrentService findAlertByCurrentService;

    @GetMapping
    public ResponseEntity<List<GetAlertResponse>> getAlerts() {
        List<GetAlertResponse> responses = findAlertByCurrentService.execute();
        return ResponseEntity.ok(responses);
    }
}
