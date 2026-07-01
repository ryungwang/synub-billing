package io.synub.sso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** SSO 요청/응답 DTO. */
public final class Dtos {

    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 100) String name) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}

    public record AccountResponse(String externalId, String email, String name) {}
}
