package team.startup.gwangsan.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String toString(byte[] content, String characterEncoding) {
        if (content == null || content.length == 0) {
            return "";
        }

        if (characterEncoding == null || characterEncoding.isBlank()) {
            return new String(content, StandardCharsets.UTF_8);
        }

        try {
            return new String(content, characterEncoding);
        } catch (Exception e) {
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    private String formatBody(String body, String contentType) {
        if (body == null || body.isBlank()) {
            return "";
        }

        if (contentType != null && contentType.contains("application/json")) {
            try {
                Object json = OBJECT_MAPPER.readValue(body, Object.class);
                return OBJECT_MAPPER
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(json);
            } catch (Exception ignore) {
                // fall through and return raw body
            }
        }

        return body;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        log.info("=========================");
        log.info("client ip = {}", req.getRemoteAddr());
        log.info("request method = {}", req.getMethod());
        log.info("request url = {}", req.getRequestURI());
        log.info("client info = {}", req.getHeader("User-Agent"));

        req.getHeaderNames().asIterator()
                .forEachRemaining(name -> log.info("header {} = {}", name, req.getHeader(name)));

        try {
            filterChain.doFilter(req, res);

            String requestBody = toString(req.getContentAsByteArray(), req.getCharacterEncoding());
            String prettyRequestBody = formatBody(requestBody, req.getContentType());
            if (!prettyRequestBody.isBlank()) {
                log.info("request body =\n{}", prettyRequestBody);
            }

            String responseBody = toString(res.getContentAsByteArray(), res.getCharacterEncoding());
            String prettyResponseBody = formatBody(responseBody, res.getContentType());
            log.info("response status = {}", res.getStatus());
            if (!prettyResponseBody.isBlank()) {
                log.info("response body =\n{}", prettyResponseBody);
            }

            res.copyBodyToResponse();
        } catch (Exception e) {
            log.error("=========================");
            log.error("error = {}", e.getMessage());
            log.error("=========================");
        }

        log.info("=========================");
    }
}
