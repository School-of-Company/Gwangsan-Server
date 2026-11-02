package team.startup.gwangsan.adapter.stream.chat;

public final class ChatStreamField {
    public static final String MESSAGE_ID = "messageId";
    public static final String ROOM_ID = "roomId";
    public static final String SENDER_ID = "senderId";
    public static final String CONTENT = "content";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String IMAGE_IDS = "imageIds";
    public static final String ATTEMPT = "attempt";
    public static final String CREATED_AT = "createdAt";
    // W3C trace context. 없는 레거시 record 도 그대로 처리한다.
    public static final String TRACEPARENT = "traceparent";
    public static final String TRACESTATE = "tracestate";

    private ChatStreamField() {}
}
