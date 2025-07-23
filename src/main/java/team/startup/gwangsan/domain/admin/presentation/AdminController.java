package team.startup.gwangsan.domain.admin.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.presentation.dto.request.AdminSignInRequest;
import team.startup.gwangsan.domain.admin.presentation.dto.request.UpdateMemberRoleRequest;
import team.startup.gwangsan.domain.admin.presentation.dto.request.UpdateMemberStatusRequest;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.domain.admin.presentation.dto.response.SignInAdminResponse;
import team.startup.gwangsan.domain.admin.service.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FindAlertByAlertTypeAndPlaceService findAlertByAlertTypeService;
    private final UpdateMemberRoleService updateMemberRoleService;
    private final SignInAdminService signInAdminService;
    private final UpdateMemberStatusService updateMemberStatusService;
    private final CompleteTradeService completeTradeService;
    private final RejectAdminAlertService rejectAdminAlertService;
    private final VerificationSignUpService verificationSignUpService;
    private final WithDrawnMemberService withDrawnMemberService;
    private final DeleteAdminAlertService deleteAdminAlertService;

    @GetMapping("/alert")
    public ResponseEntity<GetAdminAlertResponse> getAdminAlert(
            @RequestParam(name = "alert_type", required = false) AlertType type,
            @RequestParam(name = "place_name", required = false) String placeName
    ) {
        GetAdminAlertResponse response = findAlertByAlertTypeService.execute(placeName, type);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/role/{member_id}")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable("member_id") Long memberId,
            @RequestBody @Valid UpdateMemberRoleRequest request
    ) {
        updateMemberRoleService.execute(memberId, request.role());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/status/{member_id}")
    public ResponseEntity<Void> updateMemberStatus(
            @PathVariable("member_id") Long memberId,
            @RequestBody @Valid UpdateMemberStatusRequest request
    ) {
        updateMemberStatusService.execute(memberId, request.status());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInAdminResponse> signIn(@RequestBody @Valid AdminSignInRequest request) {
        SignInAdminResponse response = signInAdminService.execute(request.nickname(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trade-complete/{product_id}")
    public ResponseEntity<Void> tradeComplete(@PathVariable("product_id") Long productId) {
        completeTradeService.execute(productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/alert/{alert_id}")
    public ResponseEntity<Void> rejectAlert(@PathVariable("alert_id") Long alertId) {
        rejectAdminAlertService.execute(alertId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/verify/signup/{alert_id}")
    public ResponseEntity<Void> verifySignUp(@PathVariable("alert_id") Long alertId) {
        verificationSignUpService.execute(alertId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/ban/{member_id}")
    public ResponseEntity<Void> ban(@PathVariable("member_id") Long memberId) {
        withDrawnMemberService.execute(memberId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{alert_id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable("alert_id") Long alertId) {
        deleteAdminAlertService.execute(alertId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
