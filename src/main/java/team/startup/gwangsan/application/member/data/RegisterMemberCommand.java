package team.startup.gwangsan.application.member.data;

import java.util.List;

public record RegisterMemberCommand(String name, String nickname, String passwordHash, String phoneNumber,
                                    Long recommenderId, Integer dongId, Integer placeId, String description,
                                    List<String> specialties) {
}
