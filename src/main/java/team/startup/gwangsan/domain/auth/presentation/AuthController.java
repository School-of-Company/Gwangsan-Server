package team.startup.gwangsan.domain.auth.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.auth.presentation.dto.request.ResetPasswordRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignInRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignUpRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.MemberInfoResponse;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.service.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignUpService signUpService;
    private final SignInService signInService;
    private final ReissueTokenService reissueTokenService;
    private final SignOutService signOutService;
    private final TokenAuthenticationService tokenAuthenticationService;
    private final ResetPasswordService resetPasswordService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody @Valid SignUpRequest request) {
        signUpService.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/signin")
    public ResponseEntity<TokenResponse> signIn(@RequestBody @Valid SignInRequest request) {
        TokenResponse response = signInService.execute(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/reissue")
    public ResponseEntity<TokenResponse> reissueToken(@RequestHeader("RefreshToken") String refreshToken) {
        TokenResponse tokenResponse = reissueTokenService.execute(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }

    @DeleteMapping("/signout")
    public ResponseEntity<Void> signout(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization").substring(7);
        signOutService.execute(accessToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<MemberInfoResponse> getMemberInfo() {
        MemberInfoResponse response = tokenAuthenticationService.execute();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        resetPasswordService.execute(request);
        return ResponseEntity.ok().build();
    }
}

