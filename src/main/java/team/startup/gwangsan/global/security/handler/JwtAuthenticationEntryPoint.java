package team.startup.gwangsan.global.security.handler;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.NoOpAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint extends NoOpAuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.info("=====AUTHENTICATION DENIED=====");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
