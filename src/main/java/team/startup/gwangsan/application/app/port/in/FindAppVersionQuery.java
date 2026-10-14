package team.startup.gwangsan.application.app.port.in;

import team.startup.gwangsan.application.app.data.AppVersionResult;

public interface FindAppVersionQuery {
    AppVersionResult query(String platform);
}
