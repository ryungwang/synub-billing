package io.synub.sso.service;

import com.nimbusds.jose.JOSEException;
import io.synub.sso.crypto.JwtIssuer;
import io.synub.sso.domain.Account;
import io.synub.sso.dto.Dtos.AccountResponse;
import io.synub.sso.dto.Dtos.LoginRequest;
import io.synub.sso.dto.Dtos.RegisterRequest;
import io.synub.sso.dto.Dtos.TokenResponse;
import io.synub.sso.repo.AccountRepository;
import io.synub.sso.web.SsoExceptions.EmailTakenException;
import io.synub.sso.web.SsoExceptions.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 통합계정 가입·로그인. 비밀번호는 BCrypt 해시로만 저장, 로그인 성공 시 RS256 토큰 발급. */
@Service
public class AuthService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    public AuthService(AccountRepository accounts, PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
    }

    @Transactional
    public AccountResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (accounts.existsByEmail(email)) {
            throw new EmailTakenException("이미 가입된 이메일입니다.");
        }
        String externalId = "usr_" + UUID.randomUUID().toString().replace("-", "");
        Account account = new Account(externalId, email, passwordEncoder.encode(req.password()), req.name());
        accounts.save(account);
        return new AccountResponse(account.getExternalId(), account.getEmail(), account.getName());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        Account account = accounts.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!"active".equals(account.getStatus())
                || !passwordEncoder.matches(req.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        try {
            String token = jwtIssuer.issue(account);
            return new TokenResponse(token, "Bearer", jwtIssuer.ttlSeconds());
        } catch (JOSEException e) {
            throw new IllegalStateException("토큰 발급 실패", e);
        }
    }
}
