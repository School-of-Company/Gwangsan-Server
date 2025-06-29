package team.startup.gwangsan.domain.auth.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignInRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignUpRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.service.SignInService;
import team.startup.gwangsan.domain.auth.service.SignUpService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignUpService signUpService;
    private final SignInService signInService;

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
}

