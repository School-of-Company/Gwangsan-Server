package team.startup.gwangsan.adapter.web.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.admin.dto.request.AdjustGwangsanRequest;
import team.startup.gwangsan.adapter.web.admin.dto.request.AdminSignInRequest;
import team.startup.gwangsan.adapter.web.admin.dto.request.UpdateMemberRoleRequest;
import team.startup.gwangsan.adapter.web.admin.dto.request.UpdateMemberStatusRequest;
import team.startup.gwangsan.adapter.web.admin.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.adapter.web.admin.dto.response.SignInAdminResponse;
import team.startup.gwangsan.application.admin.data.*;
import team.startup.gwangsan.application.admin.port.in.*;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FindAdminAlertsQuery findAdminAlertsQuery;
    private final UpdateMemberRoleUseCase updateMemberRoleUseCase;
    private final SignInAdminUseCase signInAdminUseCase;
    private final UpdateMemberStatusUseCase updateMemberStatusUseCase;
    private final RejectAdminAlertUseCase rejectAdminAlertUseCase;
    private final VerifySignUpUseCase verifySignUpUseCase;
    private final DeleteAdminAlertUseCase deleteAdminAlertUseCase;
    private final ApproveTradeCancelUseCase approveTradeCancelUseCase;
    private final AdjustGwangsanUseCase adjustGwangsanUseCase;

    @GetMapping("/alert")
    public ResponseEntity<GetAdminAlertResponse> getAdminAlert(
            ActorContext actor,
            @RequestParam(name = "alert_type", required = false) AlertType type,
            @RequestParam(name = "place_id", required = false) Integer placeId
    ) {
        AdminAlertsResult result = findAdminAlertsQuery.query(new AdminAlertSearch(actor, placeId, type));
        return ResponseEntity.ok(AdminWebMapper.toResponse(result));
    }

    @PatchMapping("/role/{member_id}")
    public ResponseEntity<Void> updateMemberRole(
            ActorContext actor,
            @PathVariable("member_id") Long memberId,
            @RequestBody @Valid UpdateMemberRoleRequest request
    ) {
        updateMemberRoleUseCase.execute(new UpdateMemberRoleCommand(actor, memberId,
                ActorContext.Role.valueOf(request.role().name()), request.placeId()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/status/{member_id}")
    public ResponseEntity<Void> updateMemberStatus(
            ActorContext actor,
            @PathVariable("member_id") Long memberId,
            @RequestBody @Valid UpdateMemberStatusRequest request
    ) {
        updateMemberStatusUseCase.execute(new UpdateMemberStatusCommand(actor, memberId,
                AccountStatus.valueOf(request.status().name())));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/gwangsan/{member_id}")
    public ResponseEntity<Void> adjustGwangsan(
            ActorContext actor,
            @PathVariable("member_id") Long memberId,
            @RequestBody @Valid AdjustGwangsanRequest request
    ) {
        adjustGwangsanUseCase.execute(new AdjustGwangsanCommand(actor, memberId, request.gwangsan()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInAdminResponse> signIn(@RequestBody @Valid AdminSignInRequest request) {
        AdminSignInResult result = signInAdminUseCase.execute(new AdminSignInCommand(request.nickname(), request.password()));
        return ResponseEntity.ok(AdminWebMapper.toResponse(result));
    }

    @DeleteMapping("/alert/{alert_id}")
    public ResponseEntity<Void> rejectAlert(ActorContext actor, @PathVariable("alert_id") Long alertId) {
        rejectAdminAlertUseCase.execute(new AdminAlertCommand(actor, alertId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/verify/signup/{alert_id}")
    public ResponseEntity<Void> verifySignUp(ActorContext actor, @PathVariable("alert_id") Long alertId) {
        verifySignUpUseCase.execute(new AdminAlertCommand(actor, alertId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{alert_id}")
    public ResponseEntity<Void> deleteAlert(ActorContext actor, @PathVariable("alert_id") Long alertId) {
        deleteAdminAlertUseCase.execute(new AdminAlertCommand(actor, alertId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/trade/{alert_id}")
    public ResponseEntity<Void> approveTradeCancel(ActorContext actor, @PathVariable("alert_id") Long alertId) {
        approveTradeCancelUseCase.execute(new AdminAlertCommand(actor, alertId));
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
