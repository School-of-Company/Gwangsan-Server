package team.startup.gwangsan.application.sms.service;

import team.startup.gwangsan.application.sms.port.out.SmsCodeStorePort;
import team.startup.gwangsan.domain.sms.policy.SmsAuthPolicy;

/** 시도 횟수를 올리고 코드를 저장한다. 횟수 초과면 아무것도 저장하지 않는다. */
final class IssueCode {

    private IssueCode() {
    }

    static void save(SmsCodeStorePort store, String phoneNumber, String code) {
        countAttempt(store, phoneNumber);
        store.saveCode(phoneNumber, code, SmsAuthPolicy.CODE_TTL);
    }

    static void countAttempt(SmsCodeStorePort store, String phoneNumber) {
        int attempt = SmsAuthPolicy.nextAttempt(store.findAttempt(phoneNumber).orElse(null));
        store.saveAttempt(phoneNumber, attempt, SmsAuthPolicy.CODE_TTL);
    }
}
