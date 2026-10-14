package team.startup.gwangsan.application.auth.data;

import java.util.List;

public record SignUpCommand(String name, String nickname, String password, String phoneNumber, String dongName,
                            Integer placeId, List<String> specialties, String recommender, String description) {
}
