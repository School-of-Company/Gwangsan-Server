package team.startup.gwangsan.global.event;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;

public record CreateAdminAlertEvent(
        AlertType type,
        Long sourceId,
        Member member,
        Member otherMember
) {
    public CreateAdminAlertEvent(AlertType type, Long sourceId, Member member) {
        this(type, sourceId, member, null);
    }
}
