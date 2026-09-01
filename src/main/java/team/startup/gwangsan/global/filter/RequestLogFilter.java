package team.startup.gwangsan.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ponytail: 본문 로깅 상한. 넘으면 잘라서 찍는다.
    // 이게 없으면 채팅방 목록 응답 수십 KB가 통째로 흘러들어 로그 파이프라인이 포화되고,
    // 뒤따르는 로그가 통째로 유실된다.
    private static final int MAX_BODY_LOG_LENGTH = 2000;

    private static final String SENSITIVE_FIELDS_PATTERN;

    static {
        String fields = String.join("|",
            "password", "newPassword", "phoneNumber", "code",
            "deviceToken", "deviceId", "accessToken", "refreshToken"
        );
        SENSITIVE_FIELDS_PATTERN = "(?i)(\"(?:" + fields + ")\"\\s*:\\s*)\"[^\"]*\"";
    }

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
                return OBJECT_MAPPER.writeValueAsString(json);
            } catch (Exception ignore) {
            }
        }

        return body;
    }

    private String truncate(String body) {
        if (body.length() <= MAX_BODY_LOG_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LOG_LENGTH) + "...(truncated, total " + body.length() + " chars)";
    }

    private String maskBody(String body) {
        if (body == null || body.isBlank()) return body;
        return body.replaceAll(SENSITIVE_FIELDS_PATTERN, "$1\"****\"");
    }

    private String bodyForLog(byte[] content, String characterEncoding, String contentType) {
        return truncate(maskBody(formatBody(toString(content, characterEncoding), contentType)));
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        long startedAt = System.currentTimeMillis();

        try {
            filterChain.doFilter(req, res);
        } catch (Exception e) {
            log.error("{} {} failed. client={}", req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), e);
            throw e;
        } finally {
            String requestBody = bodyForLog(req.getContentAsByteArray(), req.getCharacterEncoding(), req.getContentType());
            String responseBody = bodyForLog(res.getContentAsByteArray(), res.getCharacterEncoding(), res.getContentType());

            log.info("{} {} -> {} ({}ms) client={} ua={} requestBody={} responseBody={}",
                    req.getMethod(),
                    req.getRequestURI(),
                    res.getStatus(),
                    System.currentTimeMillis() - startedAt,
                    req.getRemoteAddr(),
                    req.getHeader("User-Agent"),
                    requestBody,
                    responseBody
            );

            res.copyBodyToResponse();
        }
    }
}
