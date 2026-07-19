package com.melih.omsslcm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melih.omsslcm.domain.AuthToken;
import com.melih.omsslcm.repository.AuthTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Guards /api/v1/** (except /api/v1/auth/**, see WebConfig) with a simple
 * bearer-token check against AUTH_TOKEN. Deliberately not Spring Security -
 * a hand-rolled interceptor is enough for this one rule and avoids pulling in
 * a full filter-chain framework that would need reconfiguring to NOT lock
 * down every endpoint by default.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthTokenRepository authTokenRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;

        Optional<AuthToken> authToken = token == null ? Optional.empty() : authTokenRepository.findById(token);

        if (authToken.isEmpty() || authToken.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            objectMapper.writeValue(response.getWriter(), Map.of("message", "Missing or invalid token"));
            return false;
        }
        return true;
    }
}
