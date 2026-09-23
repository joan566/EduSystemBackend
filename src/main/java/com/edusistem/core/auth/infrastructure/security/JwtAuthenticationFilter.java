package com.edusistem.core.auth.infrastructure.security;

import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserRepositoryPort users;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserRepositoryPort users) {
        this.tokenProvider = tokenProvider;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            tokenProvider.parse(header.substring(PREFIX.length()).trim()).ifPresent(this::authenticateIfStillValid);
        }
        chain.doFilter(request, response);
    }

    /** El token solo vale si el usuario sigue activo y no se revocaron sus tokens (logout, cambio de contraseña...). */
    private void authenticateIfStillValid(JwtTokenProvider.ParsedToken parsed) {
        users.findAuthState(parsed.user().id())
                .filter(state -> state.active() && state.tokenVersion() == parsed.version())
                .ifPresent(state -> authenticate(parsed.user()));
    }

    private void authenticate(AuthenticatedUser user) {
        var authorities = user.roles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
