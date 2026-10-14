package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;

public record AdminCredential(String phoneNumber, String encodedPassword, ActorContext.Role role, AccountStatus status) {
}
