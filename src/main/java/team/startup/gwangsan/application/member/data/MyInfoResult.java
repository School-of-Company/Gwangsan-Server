package team.startup.gwangsan.application.member.data;

import java.util.List;

public record MyInfoResult(Long memberId, String nickname, String placeName, String headName, String dongName,
                           Integer light, Integer gwangsan, String description, List<String> specialties) {
}
