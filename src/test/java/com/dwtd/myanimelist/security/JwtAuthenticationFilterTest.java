package com.dwtd.myanimelist.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/anime");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinue_whenAuthorizationHeaderMissing() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinue_whenHeaderIsNotBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldSetAuthentication_whenTokenIsValid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("Alice");
        when(jwtService.isTokenValid("valid-token", "Alice")).thenReturn(true);
        when(jwtService.extractRole("valid-token")).thenReturn("ROLE_USER");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("Alice");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthentication_whenTokenIsInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.extractUsername("invalid-token")).thenReturn("Alice");
        when(jwtService.isTokenValid("invalid-token", "Alice")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinue_whenJwtExceptionThrown() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken");
        when(jwtService.extractUsername("broken")).thenThrow(new JwtException("bad token"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkip_whenUsernameEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("");

        filter.doFilter(request, response, filterChain);

        verify(jwtService, never()).isTokenValid(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkip_whenAlreadyAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("Alice", null, List.of())
        );
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("Alice");

        filter.doFilter(request, response, filterChain);

        verify(jwtService, never()).isTokenValid(any(), any());
        verify(filterChain).doFilter(request, response);
    }
}
