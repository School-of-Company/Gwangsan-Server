package team.startup.gwangsan.domain.member.service;

import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

public interface MemberDeletionService {
    void delete(MemberJpaEntity member);
}