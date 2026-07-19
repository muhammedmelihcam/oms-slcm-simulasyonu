package com.melih.omsslcm.service;

import com.melih.omsslcm.domain.AppUser;
import com.melih.omsslcm.domain.AuthToken;
import com.melih.omsslcm.exception.EmailAlreadyExistsException;
import com.melih.omsslcm.exception.InvalidCredentialsException;
import com.melih.omsslcm.repository.AppUserRepository;
import com.melih.omsslcm.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long TOKEN_VALIDITY_HOURS = 4;

    private final AppUserRepository appUserRepository;
    private final AuthTokenRepository authTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public void signup(String email, String rawPassword) {
        if (appUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already registered: " + email);
        }
        appUserRepository.save(AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public String signin(String email, String rawPassword) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        AuthToken token = AuthToken.builder()
                .token(UUID.randomUUID().toString())
                .email(email)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS))
                .build();
        authTokenRepository.save(token);
        return token.getToken();
    }
}
