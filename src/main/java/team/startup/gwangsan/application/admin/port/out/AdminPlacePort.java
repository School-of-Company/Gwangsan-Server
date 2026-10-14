package team.startup.gwangsan.application.admin.port.out;

import java.util.List;

public interface AdminPlacePort {

    List<Integer> placeIdsInHead(Integer headId);

    /** 지점이 없으면 PlaceNotFound. */
    Integer requirePlace(Integer placeId);
}
