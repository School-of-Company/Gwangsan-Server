package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.MyInfoResult;
import team.startup.gwangsan.domain.common.ActorContext;

public interface FindMyInfoQuery {

    MyInfoResult query(ActorContext actor);
}
