package team.startup.gwangsan.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        log.info("=========================");

        String[] logMessages = {
                "client ip = " + request.getRemoteAddr(),
                "request method = " + request.getMethod(),
                "request url = " + request.getRequestURI(),
                "client info = " + request.getHeader("User-Agent")
        };

        for (String message : logMessages) {
            log.info(message);
        }

        log.info("=========================");

        try {
            filterChain.doFilter(request, response);
            log.info("=========================");
            log.info("response status = " + response.getStatus());
            log.info("=========================");
        } catch (Exception e) {
            log.error("=========================");
            log.error(e.getCause().toString());
            log.error(e.getMessage());
            log.error(e.getCause().getMessage());
            log.error("=========================");
        }
    }
}

