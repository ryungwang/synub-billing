package io.synub.sso.web;

import io.synub.sso.dto.Dtos.AccountResponse;
import io.synub.sso.dto.Dtos.LoginRequest;
import io.synub.sso.dto.Dtos.RegisterRequest;
import io.synub.sso.dto.Dtos.TokenResponse;
import io.synub.sso.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 통합계정 가입/로그인. 로그인 성공 시 타 서비스가 검증할 액세스 토큰(JWT)을 발급한다. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
