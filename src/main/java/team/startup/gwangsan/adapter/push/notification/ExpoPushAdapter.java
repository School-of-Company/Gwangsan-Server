package team.startup.gwangsan.adapter.push.notification;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import team.startup.gwangsan.application.notification.port.out.PushSenderPort;
import team.startup.gwangsan.domain.notification.model.NotificationType;
import team.startup.gwangsan.domain.notification.model.PushMessage;
import team.startup.gwangsan.domain.notification.model.PushOutcome;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Expo Push API 연동. Expo 의 요청/응답/오류 규격은 이 클래스 밖으로 나가지 않는다.
 *
 * <p>실패 분류: 연결 오류·timeout·HTTP 429/5xx·ticket {@code MessageRateExceeded} 는 일시 실패로 보고
 * 공용 RetryTemplate(기존 3회, 1초) 안에서 재시도한다. ticket 일시 실패는 실패한 토큰만 다시 보낸다.
 * 그 밖의 HTTP 4xx 와 ticket 오류(DeviceNotRegistered 등)는 영구 실패로 보고 재시도하지 않는다.
 * timeout 후 재시도는 중복 푸시를 만들 수 있다. 영속 큐는 두지 않고 최종 실패는 로그로 남긴다.
 */
@Slf4j
@Component
public class ExpoPushAdapter implements PushSenderPort {

    private static final int CHUNK_SIZE = 100;
    private static final Set<String> TRANSIENT_TICKET_ERRORS = Set.of("MessageRateExceeded");

    private final RetryTemplate retryTemplate;
    private final WebClient expoClient;
    private final Gson gson;

    public ExpoPushAdapter(RetryTemplate retryTemplate,
                           WebClient.Builder webClientBuilder,
                           Gson gson,
                           @Value("${expo.base-url:https://exp.host}") String baseUrl) {
        this.retryTemplate = retryTemplate;
        this.gson = gson;
        this.expoClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public PushOutcome send(PushMessage message, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return PushOutcome.NO_TARGET;
        if (message.type() == null) {
            log.warn("[Expo] NotificationType이 null이므로 푸시 알림 전송을 중단합니다.");
            return PushOutcome.NO_TARGET;
        }

        List<String> expoTokens = expoTokens(tokens);
        if (expoTokens.isEmpty()) {
            log.warn("[Expo] 대상 디바이스 토큰 {}건 중 Expo 형식(ExponentPushToken[...]) 토큰이 없어 전송을 건너뜁니다.", tokens.size());
            return PushOutcome.NO_TARGET;
        }

        Map<String, String> data = buildData(message.type(), message.sourceId());
        boolean failed = false;
        for (int i = 0; i < expoTokens.size(); i += CHUNK_SIZE) {
            List<String> chunk = expoTokens.subList(i, Math.min(i + CHUNK_SIZE, expoTokens.size()));
            if (!sendChunk(chunk, message, data)) {
                failed = true;
            }
        }
        return failed ? PushOutcome.FAILED : PushOutcome.SENT;
    }

    private boolean sendChunk(List<String> chunk, PushMessage message, Map<String, String> data) {
        AtomicReference<List<String>> targets = new AtomicReference<>(chunk);
        AtomicBoolean permanentFailure = new AtomicBoolean(false);
        try {
            retryTemplate.execute(ctx -> {
                String response;
                try {
                    response = post(targets.get(), message, data);
                } catch (WebClientResponseException e) {
                    if (isTransient(e.getStatusCode())) throw e;
                    log.warn("[Expo] HTTP 영구 오류로 재시도하지 않습니다. status={}", e.getStatusCode().value());
                    permanentFailure.set(true);
                    return null;
                }
                TicketResult result = readTickets(response, targets.get());
                if (result.permanentErrors() > 0) permanentFailure.set(true);
                if (!result.retryTokens().isEmpty()) {
                    targets.set(result.retryTokens());
                    throw new TransientTicketException(result.retryTokens().size());
                }
                return null;
            });
        } catch (Exception e) {
            log.error("[Expo] 최종 실패: kind=TRANSIENT, type={}, sourceId={}, tokens={}, error_type={}",
                    message.type(), message.sourceId(), targets.get().size(), e.getClass().getName(), e);
            return false;
        }
        if (permanentFailure.get()) {
            log.error("[Expo] 최종 실패: kind=PERMANENT, type={}, sourceId={}, tokens={}",
                    message.type(), message.sourceId(), chunk.size());
            return false;
        }
        return true;
    }

    private String post(List<String> tokens, PushMessage message, Map<String, String> data) {
        List<Map<String, Object>> payload = tokens.stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("to", t);
                    m.put("title", message.title());
                    m.put("body", message.body());
                    m.put("sound", "default");
                    m.put("data", data);
                    return m;
                })
                .toList();
        return expoClient.post()
                .uri("/--/api/v2/push/send")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));
    }

    static boolean isTransient(HttpStatusCode status) {
        return status.value() == 429 || status.is5xxServerError();
    }

    boolean isExpoPushToken(String t) {
        return t != null && t.startsWith("ExponentPushToken[");
    }

    Map<String, String> buildData(NotificationType type, Long sourceId) {
        Map<String, String> data = new HashMap<>();
        data.put("alertType", type.name());
        if (sourceId != null) {
            data.put("sourceId", String.valueOf(sourceId));
            if (type == NotificationType.FEEDBACK_ANSWER) {
                data.put("feedbackId", String.valueOf(sourceId));
            }
            if (type == NotificationType.CHATTING || type == NotificationType.RESERVATION_REMINDER
                    || type == NotificationType.RESERVATION_CANCEL) {
                data.put("roomId", String.valueOf(sourceId));
            }
        }
        return data;
    }

    List<String> expoTokens(List<String> tokens) {
        return tokens.stream()
                .filter(Objects::nonNull)
                .filter(this::isExpoPushToken)
                .distinct()
                .toList();
    }

    /**
     * ticket 은 요청 순서와 같은 순서로 온다. 개수가 다르면 토큰을 특정할 수 없으므로 일시 오류도 재시도하지 않는다.
     * 응답을 해석할 수 없으면 기존처럼 오류 없음으로 본다.
     */
    TicketResult readTickets(String response, List<String> tokens) {
        if (response == null || response.isBlank()) return TicketResult.OK;
        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            if (root == null || !root.has("data") || !root.get("data").isJsonArray()) return TicketResult.OK;

            JsonArray tickets = root.getAsJsonArray("data");
            boolean aligned = tickets.size() == tokens.size();
            int permanent = 0;
            List<String> retry = new ArrayList<>();
            for (int i = 0; i < tickets.size(); i++) {
                JsonElement element = tickets.get(i);
                if (!element.isJsonObject()) continue;
                JsonObject ticket = element.getAsJsonObject();
                if (!"error".equals(asString(ticket, "status"))) continue;

                JsonObject details = ticket.has("details") && ticket.get("details").isJsonObject()
                        ? ticket.getAsJsonObject("details")
                        : null;
                String error = details == null ? null : asString(details, "error");
                // ticket message/details 에는 device token 이 들어 있어 error 코드만 남긴다.
                log.warn("[Expo] ticket error: error={}", error);
                if (aligned && TRANSIENT_TICKET_ERRORS.contains(error)) {
                    retry.add(tokens.get(i));
                } else {
                    permanent++;
                }
            }
            return new TicketResult(permanent, retry);
        } catch (Exception e) {
            log.warn("[Expo] 응답 파싱 실패: error_type={}", e.getClass().getName());
            return TicketResult.OK;
        }
    }

    private String asString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    record TicketResult(int permanentErrors, List<String> retryTokens) {
        static final TicketResult OK = new TicketResult(0, List.of());
    }

    static class TransientTicketException extends RuntimeException {
        TransientTicketException(int count) {
            super("Expo ticket 일시 실패 " + count + "건");
        }
    }
}
