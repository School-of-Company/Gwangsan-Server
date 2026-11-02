package team.startup.gwangsan.application.member.port.out;

public interface ValidatePlacePort {

    /** 지점이 없으면 PlaceNotFound. */
    void requireExists(Integer placeId);

    /** 지점이 해당 본부 소속이면 true. 지점이 없으면 false. */
    boolean belongsToHead(Integer placeId, Integer headId);
}
