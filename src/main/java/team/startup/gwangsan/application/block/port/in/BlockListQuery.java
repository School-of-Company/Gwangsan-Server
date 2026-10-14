package team.startup.gwangsan.application.block.port.in;

import team.startup.gwangsan.application.block.data.BlockedMember;

import java.util.List;

public interface BlockListQuery {
    List<BlockedMember> query(Long actorId);
}
