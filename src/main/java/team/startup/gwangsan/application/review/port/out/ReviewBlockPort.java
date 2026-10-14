package team.startup.gwangsan.application.review.port.out;

public interface ReviewBlockPort {
    void validateNotBlocked(long memberId, long otherMemberId);
}
