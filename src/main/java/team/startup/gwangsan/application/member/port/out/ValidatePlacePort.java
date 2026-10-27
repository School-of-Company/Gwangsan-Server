package team.startup.gwangsan.application.member.port.out;

public interface ValidatePlacePort {

    /** 지점이 없으면 PlaceNotFound. */
    void requireExists(Integer placeId);
}
