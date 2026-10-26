package ypfunding.server.config;

import lombok.Getter;
import ypfunding.common.config.AppProperties;

public class ServerAppProperties {
    //설정파일 경로
    public static final String DEFAULT_PATH = "config/application.properties";

    public static final String KEY_DB_URL = "db.url";
    public static final String KEY_DB_USERNAME = "db.username";
    public static final String KEY_DB_PASSWORD = "db.password";

    @Getter
    private final String dbUrl;
    @Getter
    private final String dbUsername;
    @Getter
    private final String dbPassword;

    private ServerAppProperties(String dbUrl, String dbUsername, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    //외부에서 부를 것
    public static ServerAppProperties load(String resourcePath) {

        AppProperties appProperties = AppProperties.load(ServerAppProperties.class, resourcePath);

        return new ServerAppProperties(
                appProperties.getValue(KEY_DB_URL),
                appProperties.getValue(KEY_DB_USERNAME),
                appProperties.getValue(KEY_DB_PASSWORD)
        );
    }

    public String toString() {
        return "AppProperties{dbUrl: " + dbUrl + ", dbUsername: " + dbUsername + "}";
    }
}
