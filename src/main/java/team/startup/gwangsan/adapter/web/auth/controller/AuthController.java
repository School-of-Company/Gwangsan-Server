package team.startup.gwangsan.adapter.web.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.auth.dto.request.FindNicknameRequest;
import team.startup.gwangsan.adapter.web.auth.dto.request.ResetPasswordRequest;
import team.startup.gwangsan.adapter.web.auth.dto.request.SignInRequest;
import team.startup.gwangsan.adapter.web.auth.dto.request.SignUpRequest;
import team.startup.gwangsan.adapter.web.auth.dto.response.FindNicknameResponse;
import team.startup.gwangsan.adapter.web.auth.dto.response.MemberInfoResponse;
import team.startup.gwangsan.adapter.web.auth.dto.response.TokenResponse;
import team.startup.gwangsan.application.auth.data.CurrentMemberResult;
import team.startup.gwangsan.application.auth.data.ResetPasswordCommand;
import team.startup.gwangsan.application.auth.data.SignInCommand;
import team.startup.gwangsan.application.auth.data.SignUpCommand;
import team.startup.gwangsan.application.auth.data.TokenResult;
import team.startup.gwangsan.application.auth.port.in.CurrentMemberQuery;
import team.startup.gwangsan.application.auth.port.in.FindNicknameQuery;
import team.startup.gwangsan.application.auth.port.in.ReissueTokenUseCase;
import team.startup.gwangsan.application.auth.port.in.ResetPasswordUseCase;
import team.startup.gwangsan.application.auth.port.in.SignInUseCase;
import team.startup.gwangsan.application.auth.port.in.SignOutUseCase;
import team.startup.gwangsan.application.auth.port.in.SignUpUseCase;
import team.startup.gwangsan.domain.common.ActorContext;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignUpUseCase signUpUseCase;
    private final SignInUseCase signInUseCase;
    private final ReissueTokenUseCase reissueTokenUseCase;
    private final SignOutUseCase signOutUseCase;
    private final CurrentMemberQuery currentMemberQuery;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final FindNicknameQuery findNicknameQuery;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody @Valid SignUpRequest request) {
        signUpUseCase.execute(new SignUpCommand(request.name(), request.nickname(), request.password(),
                request.phoneNumber(), request.dongName(), request.placeId(), request.specialties(),
                request.recommender(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/signin")
    public ResponseEntity<TokenResponse> signIn(@RequestBody @Valid SignInRequest request) {
        TokenResult result = signInUseCase.execute(new SignInCommand(request.nickname(), request.password(),
                request.deviceToken(), request.deviceId(), request.osType() == null ? null : request.osType().name()));
        return ResponseEntity.ok(toResponse(result));
    }

    @PatchMapping("/reissue")
    public ResponseEntity<TokenResponse> reissueToken(@RequestHeader("RefreshToken") String refreshToken) {
        return ResponseEntity.ok(toResponse(reissueTokenUseCase.execute(refreshToken)));
    }

    @DeleteMapping("/signout")
    public ResponseEntity<Void> signout(ActorContext actor, HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization").substring(7);
        signOutUseCase.execute(actor, accessToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<MemberInfoResponse> getMemberInfo(ActorContext actor) {
        CurrentMemberResult result = currentMemberQuery.query(actor);
        return ResponseEntity.ok(new MemberInfoResponse(result.memberId(), result.nickname()));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        resetPasswordUseCase.execute(new ResetPasswordCommand(request.phoneNumber(), request.newPassword()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/nickname")
    public ResponseEntity<FindNicknameResponse> findNickname(@RequestBody @Valid FindNicknameRequest request) {
        return ResponseEntity.ok(new FindNicknameResponse(findNicknameQuery.query(request.phoneNumber())));
    }

    private static TokenResponse toResponse(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken(),
                result.accessTokenExpiresIn(), result.refreshTokenExpiresIn());
    }
}
