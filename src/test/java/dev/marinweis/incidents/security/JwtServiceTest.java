package dev.marinweis.incidents.security;

import dev.marinweis.incidents.model.Role;
import dev.marinweis.incidents.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy0yNTYtYml0LW1pbmltdW0h";

    @Test
    void shouldGenerateAndValidateToken() {
        JwtService service = new JwtService(SECRET, 60_000L);
        User user = new User("alice", "alice@example.com", "hash", Role.USER);

        String token = service.generateToken(user);

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void shouldRejectTamperedOrExpiredToken() {
        JwtService expiredService = new JwtService(SECRET, -1_000L);
        User user = new User("bob", "bob@example.com", "hash", Role.ADMIN);

        String expiredToken = expiredService.generateToken(user);

        assertThat(expiredService.isValid(expiredToken)).isFalse();
        assertThat(expiredService.isValid("not-a-valid-jwt")).isFalse();
    }
}
