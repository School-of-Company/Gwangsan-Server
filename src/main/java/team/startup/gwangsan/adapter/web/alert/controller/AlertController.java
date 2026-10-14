package team.startup.gwangsan.adapter.web.alert.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.alert.dto.ExistsAlertResponse;
import team.startup.gwangsan.adapter.web.alert.dto.GetAlertResponse;
import team.startup.gwangsan.application.alert.port.in.MyAlertsQuery;
import team.startup.gwangsan.application.alert.port.in.ReadAlertsUseCase;
import team.startup.gwangsan.application.alert.port.in.UnreadAlertQuery;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final MyAlertsQuery myAlertsQuery;
    private final UnreadAlertQuery unreadAlertQuery;
    private final ReadAlertsUseCase readAlertsUseCase;

    @GetMapping
    public ResponseEntity<List<GetAlertResponse>> getAlerts(ActorContext actor) {
        return ResponseEntity.ok(myAlertsQuery.query(actor.memberId()).stream().map(GetAlertResponse::from).toList());
    }

    @GetMapping("/unread")
    public ResponseEntity<ExistsAlertResponse> getUnreadAlerts(ActorContext actor) {
        return ResponseEntity.ok(new ExistsAlertResponse(unreadAlertQuery.hasUnread(actor.memberId())));
    }

    @PatchMapping("/read/{alert_id}")
    public ResponseEntity<Void> readAlerts(ActorContext actor, @PathVariable("alert_id") Long alertId) {
        readAlertsUseCase.readUpTo(actor.memberId(), alertId);
        return ResponseEntity.noContent().build();
    }
}
