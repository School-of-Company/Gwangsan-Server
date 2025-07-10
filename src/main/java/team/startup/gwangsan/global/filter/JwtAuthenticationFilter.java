package team.startup.gwangsan.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import team.startup.gwangsan.global.auth.MemberDetails;
import team.startup.gwangsan.global.auth.MemberDetailsService;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final MemberDetailsService memberDetailsService;

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtProvider.validateAccessToken(token)) {
            String phoneNumber = jwtProvider.validateAndGetSubject(token);
            MemberDetails memberDetails = (MemberDetails) memberDetailsService.loadUserByUsername(phoneNumber);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    memberDetails, null, memberDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return pathMatcher.match("/api/auth/**", path) || pathMatcher.match("/api/health", path);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
