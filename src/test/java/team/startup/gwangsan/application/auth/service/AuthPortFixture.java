package team.startup.gwangsan.application.auth.service;

import team.startup.gwangsan.application.auth.data.IssuedToken;
import team.startup.gwangsan.domain.auth.model.Account;
import team.startup.gwangsan.domain.auth.model.AccountStatus;

final class AuthPortFixture {

    static final String PHONE = "01012345678";

    private AuthPortFixture() {
    }

    static Account account(AccountStatus status) {
        return new Account(1L, "테스터일", PHONE, "encodedPw", "ROLE_USER", status);
    }

    static IssuedToken token(String access, String refresh) {
        return new IssuedToken(access, refresh, 3600L, 86400L);
    }
}
