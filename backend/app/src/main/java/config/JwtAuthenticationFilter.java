package config;

import java.io.*;
import java.util.*;
import java.util.Locale;

import auth.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.*;
import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;
import org.springframework.util.*;
import org.springframework.web.filter.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final AuthService authService;

    public JwtAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String bearer = extractBearer(request);
            if (StringUtils.hasText(bearer)) {
                var parsed = authService.parseToken(bearer);
                SimpleGrantedAuthority authority = resolveAuthority(parsed.role());
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

    private SimpleGrantedAuthority resolveAuthority(String rawRole) {
        String role = rawRole != null ? rawRole.trim() : "user";
        String lower = role.toLowerCase(Locale.ROOT);
        boolean adminLike =
                lower.equals("admin")
                        || lower.equals("administrator")
                        || role.equalsIgnoreCase("ROLE_ADMIN");

        if (adminLike) {
            return new SimpleGrantedAuthority("ROLE_ADMIN");
        }

        String upper = role.toUpperCase(Locale.ROOT);
        if (upper.startsWith("ROLE_")) {
            return new SimpleGrantedAuthority(upper);
        }

        return new SimpleGrantedAuthority("ROLE_USER");
    }

    private String extractBearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (StringUtils.hasText(h)) {
            return h;
        }
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
