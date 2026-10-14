package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.UserInfoResult;

public interface FindUserInfoQuery {

    UserInfoResult query(Long memberId);
}
