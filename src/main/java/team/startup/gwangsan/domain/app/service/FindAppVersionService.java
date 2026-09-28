package team.startup.gwangsan.domain.app.service;

import team.startup.gwangsan.domain.app.presentation.dto.response.AppVersionResponse;

public interface FindAppVersionService {
    AppVersionResponse execute(String platform);
}
