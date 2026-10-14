package team.startup.gwangsan.application.auth.port.in;

import team.startup.gwangsan.domain.common.ActorContext;

public interface SignOutUseCase {

    void execute(ActorContext actor, String accessToken);
}
