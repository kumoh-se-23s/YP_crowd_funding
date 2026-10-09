package ypfunding.server.config;

import lombok.Getter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AppProperties {
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
    private AppProperties(String dbUrl, String dbUsername, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    //외부에서 부를 것
    public static AppProperties load(String resourcePath) {
        Properties props = new Properties();

        //인자 경로에서 파일 찾기
        try (InputStream in = AppProperties.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if(in == null) {
                throw new IllegalStateException("설정 파일을 찾을 수 없음: " + resourcePath + ".example 파일 참고해서 세팅 필요");
            }

            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException e){
            throw new IllegalStateException("읽는 중 문제 발생: "+ resourcePath, e);
        }

        String url = required(props, KEY_DB_URL, resourcePath);
        String username = required(props, KEY_DB_USERNAME, resourcePath);
        String password = present(props, KEY_DB_PASSWORD, resourcePath);

        return new AppProperties(url, username, password);
    }

    //필수 값 불러오기
    private static String required(Properties props, String key, String resourcePath) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("설정 파일에 " + key + " 값이 없음: " + resourcePath);
        }
        return value.trim();
    }

    //있어도되고 없어도되는거 불러오기
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
