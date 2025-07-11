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
import team.startup.gwangsan.domain.admin.service.FindAlertByAlertTypeAndPlaceService;
import team.startup.gwangsan.domain.admin.service.SignInAdminService;
import team.startup.gwangsan.domain.admin.service.UpdateMemberRoleService;
import team.startup.gwangsan.domain.admin.service.UpdateMemberStatusService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FindAlertByAlertTypeAndPlaceService findAlertByAlertTypeService;
    private final UpdateMemberRoleService updateMemberRoleService;
    private final SignInAdminService signInAdminService;
    private final UpdateMemberStatusService updateMemberStatusService;

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

}
