package com.ivedi.ivedikuryemanagement.realtime.impl;

import com.ivedi.ivedikuryemanagement.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message; // only validate on initial CONNECT
        }

        String token = extractBearerToken(accessor.getFirstNativeHeader("Authorization"));
        if (!isStructurallyValidJwt(token)) {
            return reject("missing or malformed Authorization header");
        }

        try {
            String username = jwtUtil.extractUsername(token);
            var userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.isTokenValid(token, userDetails)) {
                return reject("invalid or expired JWT");
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth);

        } catch (Exception e) {
            return reject("invalid or expired JWT");
        }

        return message;
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String value = authHeader.trim();
        if (!value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        return value.substring(BEARER_PREFIX.length()).trim();
    }

    private boolean isStructurallyValidJwt(String token) {
        if (token == null || token.isBlank()
                || "undefined".equalsIgnoreCase(token)
                || "null".equalsIgnoreCase(token)) {
            return false;
        }

        String[] parts = token.split("\\.", -1);
        return parts.length == 3
                && !parts[0].isBlank()
                && !parts[1].isBlank()
                && !parts[2].isBlank();
    }

    private Message<?> reject(String reason) {
        log.warn("WebSocket CONNECT rejected: {}", reason);
        throw new IllegalArgumentException("WebSocket authentication failed");
    }
}
