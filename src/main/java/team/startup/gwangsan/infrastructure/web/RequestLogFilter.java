package team.startup.gwangsan.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 1건당 access log 1줄. 요청/응답 본문, query string, header, client IP 는 남기지 않는다
 * (토큰·전화번호·채팅 본문·개인 식별자가 섞인다). trace_id/span_id 는 JSON formatter 가 붙인다.
 */
@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    // 내부 scrape/health 요청은 15초마다 와서 access log 를 채운다.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            String path = pathWithoutParameters(request.getRequestURI());
            LoggingEventBuilder event = (failure == null ? log.atInfo() : log.atError())
                    .addKeyValue("event", "http_request")
                    .addKeyValue("http_method", request.getMethod())
                    .addKeyValue("http_path", path)
                    .addKeyValue("http_status", response.getStatus())
                    .addKeyValue("duration_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            if (failure != null) {
                event = event.addKeyValue("error_type", failure.getClass().getName());
            }
            event.log("{} {} -> {}", request.getMethod(), path, response.getStatus());
        }
    }

    // ;jsessionid= 같은 path parameter 를 뗀다. getRequestURI 에는 query string 이 없다.
    private static String pathWithoutParameters(String uri) {
        if (uri == null) return "";
        int semicolon = uri.indexOf(';');
        return semicolon < 0 ? uri : uri.substring(0, semicolon);
    }
}
