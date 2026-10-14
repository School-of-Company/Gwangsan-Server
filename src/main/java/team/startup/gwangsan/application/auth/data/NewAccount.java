package team.startup.gwangsan.application.auth.data;

import java.util.List;

public record NewAccount(String name, String nickname, String passwordHash, String phoneNumber, Long recommenderId,
                         Integer dongId, Integer placeId, String description, List<String> specialties) {
}
