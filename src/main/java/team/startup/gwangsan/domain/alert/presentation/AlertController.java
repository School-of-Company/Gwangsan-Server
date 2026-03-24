package team.startup.gwangsan.domain.alert.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.alert.presentation.dto.response.GetAlertResponse;
import team.startup.gwangsan.domain.alert.presentation.dto.response.ExistsAlertResponse;
import team.startup.gwangsan.domain.alert.service.ExistsUnreadAlertService;
import team.startup.gwangsan.domain.alert.service.FindAlertByCurrentService;
import team.startup.gwangsan.domain.alert.service.ReadAlertService;

import java.util.List;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final FindAlertByCurrentService findAlertByCurrentService;
    private final ExistsUnreadAlertService existsUnreadAlertService;
    private final ReadAlertService readAlertService;

    @GetMapping
    public ResponseEntity<List<GetAlertResponse>> getAlerts() {
        List<GetAlertResponse> responses = findAlertByCurrentService.execute();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/unread")
    public ResponseEntity<ExistsAlertResponse> getUnreadAlerts() {
        ExistsAlertResponse responses = existsUnreadAlertService.execute();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/read/{alert_id}")
    public ResponseEntity<Void> readAlerts(@PathVariable("alert_id") Long alertId) {
        readAlertService.execute(alertId);
        return ResponseEntity.noContent().build();
    }
}
