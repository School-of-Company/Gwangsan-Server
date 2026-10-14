package team.startup.gwangsan.application.auth.port.in;

import team.startup.gwangsan.application.auth.data.IssuedToken;

/** 인증을 마친 계정에 토큰을 발급하고 refresh token 을 저장한다(일반 로그인과 같은 순서). 관리자 로그인이 사용한다. */
public interface IssueAccountTokenUseCase {

    IssuedToken execute(String phoneNumber, String role);
}
