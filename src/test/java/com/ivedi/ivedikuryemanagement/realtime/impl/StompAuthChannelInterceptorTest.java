package com.ivedi.ivedikuryemanagement.realtime.impl;

import com.ivedi.ivedikuryemanagement.common.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;

    @Test
    void preSend_withMalformedBearerToken_rejectsBeforeParsingJwt() {
        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtUtil, userDetailsService);
        Message<?> message = connectMessage("Bearer undefined");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebSocket authentication failed");

        verify(jwtUtil, never()).extractUsername("undefined");
    }

    @Test
    void preSend_withStructurallyValidBearerToken_authenticatesConnection() {
        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtUtil, userDetailsService);
        String token = "header.payload.signature";
        UserDetails userDetails = User.withUsername("user@example.com")
                .password("password")
                .authorities("ROLE_COURIER")
                .build();
        when(jwtUtil.extractUsername(token)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid(token, userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(connectMessage("Bearer " + token), null);

        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(resultAccessor).isNotNull();
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo("user@example.com");
    }

    private Message<?> connectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", authorizationHeader);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
