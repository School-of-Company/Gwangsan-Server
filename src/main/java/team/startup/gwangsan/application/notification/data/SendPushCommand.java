package team.startup.gwangsan.application.notification.data;

import team.startup.gwangsan.domain.notification.model.PushMessage;

import java.util.List;

public record SendPushCommand(
        List<Long> recipientMemberIds,
        PushMessage message
) {
    public SendPushCommand {
        recipientMemberIds = recipientMemberIds == null ? List.of() : List.copyOf(recipientMemberIds);
    }
}
