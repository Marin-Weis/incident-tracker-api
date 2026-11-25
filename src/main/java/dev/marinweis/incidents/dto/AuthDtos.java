package dev.marinweis.incidents.dto;

import dev.marinweis.incidents.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record UserResponse(Long id, String username, String email, Role role) {}

    public record AuthResponse(String accessToken, String tokenType, UserResponse user) {
        public static AuthResponse bearer(String token, UserResponse user) {
            return new AuthResponse(token, "Bearer", user);
        }
    }
}
