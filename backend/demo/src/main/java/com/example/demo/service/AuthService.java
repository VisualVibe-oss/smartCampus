package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.AuthSession;
import com.example.demo.entity.User;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.repository.AuthSessionRepository;
import com.example.demo.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    static final String INVALID_CREDENTIALS = "Adresse e-mail ou mot de passe incorrect.";
    static final String DISABLED_ACCOUNT = "Ce compte est désactivé. Contactez l'administration.";
    static final String SESSION_REQUIRED =
            "Authentification requise : fournissez un jeton Bearer valide.";
    static final String INVALID_SESSION =
            "Session invalide ou expirée. Veuillez vous reconnecter.";

    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Duration sessionTtl;
    private final Set<String> institutionalDomains;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, AuthSessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${smartcampus.auth.session-ttl:PT8H}") Duration sessionTtl,
            @Value("${smartcampus.auth.institutional-domains:}") String institutionalDomains) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionTtl = sessionTtl;
        this.institutionalDomains = parseDomains(institutionalDomains);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();
        checkInstitutionalDomain(email);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            throw new ForbiddenException(DISABLED_ACCOUNT);
        }

        Instant now = Instant.now();
        sessionRepository.deleteByExpiresAtBefore(now);

        String token = generateToken();
        AuthSession session = new AuthSession();
        session.setTokenHash(hashToken(token));
        session.setUser(user);
        session.setExpiresAt(now.plus(sessionTtl));
        sessionRepository.saveAndFlush(session);

        return new LoginResponse(token, "Bearer", session.getExpiresAt(), toResponse(user));
    }

    public UserResponse currentUser(String bearerToken) {
        AuthSession session = findActiveSession(bearerToken);
        return toResponse(session.getUser());
    }

    @Transactional
    public void logout(String bearerToken) {
        AuthSession session = findActiveSession(bearerToken);
        sessionRepository.delete(session);
        sessionRepository.flush();
    }

    private AuthSession findActiveSession(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new UnauthorizedException(SESSION_REQUIRED);
        }
        AuthSession session = sessionRepository
                .findByTokenHashAndExpiresAtAfter(hashToken(bearerToken.trim()), Instant.now())
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
        if (!session.getUser().isEnabled()) {
            throw new ForbiddenException(DISABLED_ACCOUNT);
        }
        return session;
    }

    private void checkInstitutionalDomain(String email) {
        if (institutionalDomains.isEmpty()) {
            return;
        }
        int at = email.lastIndexOf('@');
        String domain = at >= 0 ? email.substring(at + 1).toLowerCase(Locale.ROOT) : "";
        if (!institutionalDomains.contains(domain)) {
            throw new UnauthorizedException(
                    "Seules les adresses e-mail institutionnelles sont autorisées.");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Algorithme SHA-256 indisponible.", exception);
        }
    }

    private static Set<String> parseDomains(String rawDomains) {
        if (rawDomains == null || rawDomains.isBlank()) {
            return Set.of();
        }
        Set<String> domains = new LinkedHashSet<>();
        Arrays.stream(rawDomains.split(","))
                .map(String::trim)
                .map(domain -> domain.replaceFirst("^@", ""))
                .filter(domain -> !domain.isEmpty())
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .forEach(domains::add);
        return Set.copyOf(domains);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isEnabled(), user.getCreatedAt());
    }
}
