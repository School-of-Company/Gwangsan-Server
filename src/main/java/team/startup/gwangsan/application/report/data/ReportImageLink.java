package team.startup.gwangsan.application.report.data;

/** linkId 는 이미지 ID 가 아니라 신고-이미지 연결 행 ID 다(기존 알림 응답 계약). */
public record ReportImageLink(Long linkId, String imageUrl) {
}
