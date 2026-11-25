package dev.marinweis.incidents.service;

import dev.marinweis.incidents.dto.AuthDtos.*;
import dev.marinweis.incidents.exception.ConflictException;
import dev.marinweis.incidents.exception.ResourceNotFoundException;
import dev.marinweis.incidents.model.Role;
import dev.marinweis.incidents.model.User;
import dev.marinweis.incidents.repository.UserRepository;
import dev.marinweis.incidents.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }
        User user = userRepository.save(new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        ));
        return AuthResponse.bearer(jwtService.generateToken(user), toDto(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.username()));
        return AuthResponse.bearer(jwtService.generateToken(user), toDto(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toDto(user);
    }

    private UserResponse toDto(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
