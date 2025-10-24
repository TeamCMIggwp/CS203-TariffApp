package config;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import auth.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final AuthService authService;

    public JwtAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String bearer = extractBearer(request);
            if (StringUtils.hasText(bearer)) {
                var parsed = authService.parseToken(bearer);
                String role = parsed.role() != null ? parsed.role() : "user";
                var authority = role.equalsIgnoreCase("admin")
                        ? new SimpleGrantedAuthority("ROLE_ADMIN")
                        : new SimpleGrantedAuthority("ROLE_USER");
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        parsed.userId(), null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ex) {
            // Invalid/missing token is fine — leave context unauthenticated and let
            // Security rules handle 401/403. Do not spam logs for normal anonymous GETs.
            if (log.isDebugEnabled()) {
                log.debug("JWT filter skipped: {}", ex.toString());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (StringUtils.hasText(h)) return h;
        // Fallback: accept access_token cookie set by frontend for SSR/middleware
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                    return "Bearer " + c.getValue();
                }
            }
        }
        return null;
    }
}
