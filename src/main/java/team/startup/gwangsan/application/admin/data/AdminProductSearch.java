package team.startup.gwangsan.application.admin.data;

/** type/mode 는 post 의 Type/Mode 이름(null 이면 필터 없음). */
public record AdminProductSearch(String type, String mode, Long lastId, int size) {
}
