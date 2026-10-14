package team.startup.gwangsan.application.admin.port.out;

import team.startup.gwangsan.application.admin.data.AdminCredential;
import team.startup.gwangsan.application.admin.data.IssuedToken;
import team.startup.gwangsan.domain.common.ActorContext;

public interface AdminAuthPort {

    /** 닉네임의 회원이 없으면 NotFoundMember. */
    AdminCredential loadCredential(String nickname);

    boolean passwordMatches(String rawPassword, String encodedPassword);

    /** access/refresh 를 발급하고 refresh 를 저장한다. */
    IssuedToken issueToken(String phoneNumber, ActorContext.Role role);
}
