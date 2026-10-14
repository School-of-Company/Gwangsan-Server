package team.startup.gwangsan.application.alert.data;

import team.startup.gwangsan.domain.alert.model.Alert;

import java.util.List;

public record RecordAlertCommand(
        Alert alert,
        List<Long> recipientMemberIds
) {
    public RecordAlertCommand {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }
}
