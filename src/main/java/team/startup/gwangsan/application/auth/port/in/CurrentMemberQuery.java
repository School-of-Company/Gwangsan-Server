package team.startup.gwangsan.application.auth.port.in;

import team.startup.gwangsan.application.auth.data.CurrentMemberResult;
import team.startup.gwangsan.domain.common.ActorContext;

public interface CurrentMemberQuery {

    CurrentMemberResult query(ActorContext actor);
}
