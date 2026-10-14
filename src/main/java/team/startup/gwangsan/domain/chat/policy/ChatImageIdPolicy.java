package team.startup.gwangsan.domain.chat.policy;

import team.startup.gwangsan.domain.chat.exception.InvalidChatImageIdsException;

import java.util.List;

/**
 * 채팅 서버가 query string 으로 넘긴 이미지 ID 검증. JS Number 로 안전한 양의 정수만 허용한다.
 */
public final class ChatImageIdPolicy {

    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    private ChatImageIdPolicy() {
    }

    public static List<Long> parse(List<String> rawIds) {
        if (rawIds.isEmpty()) {
            throw new InvalidChatImageIdsException();
        }
        return rawIds.stream().map(ChatImageIdPolicy::parseOne).toList();
    }

    private static Long parseOne(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw new InvalidChatImageIdsException();
        }
        try {
            long id = Long.parseLong(value);
            if (id <= 0 || id > MAX_SAFE_INTEGER) {
                throw new InvalidChatImageIdsException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new InvalidChatImageIdsException();
        }
    }
}
