package ypfunding.server.config;

import lombok.Getter;
import ypfunding.common.config.AppProperties;

import java.util.Properties;

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
        Properties props = AppProperties.load(ServerAppProperties.class, resourcePath).getProperties();

        return new ServerAppProperties(
                required(props, KEY_DB_URL, resourcePath),
                required(props, KEY_DB_USERNAME, resourcePath),
                present(props, KEY_DB_PASSWORD, resourcePath)
        );
    }

    //필수 값 : 항목이 없거나 비어 있으면 예외
    private static String required(Properties props, String key, String resourcePath) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("설정 파일에 " + key + " 값이 없음: " + resourcePath);
        }
        return value.trim();
    }

    //항목 O, 값 X (비밀번호 없는 로컬 계정)
    private static String present(Properties props, String key, String resourcePath) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("설정 파일에 " + key + " 항목이 없음: " + resourcePath);
        }
        return value.trim();
    }

    public String toString() {
        return "AppProperties{dbUrl: " + dbUrl + ", dbUsername: " + dbUsername + "}";
    }
}