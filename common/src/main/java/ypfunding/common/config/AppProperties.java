package ypfunding.common.config;

import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AppProperties {

    @Getter
    private final Properties properties;
    private AppProperties(Properties properties) {
        this.properties = properties;
    }

    //외부에서 부를 것
    public static AppProperties load(Class<?> referenceClass, String resourcePath) {
        Properties props = new Properties();

        //인자 경로에서 파일 찾기
        try (InputStream in = referenceClass.getClassLoader().getResourceAsStream(resourcePath)) {
            if(in == null) {
                throw new IllegalStateException("설정 파일을 찾을 수 없음: " + resourcePath + ".example 파일 참고해서 세팅 필요");
            }

            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException e){
            throw new IllegalStateException("읽는 중 문제 발생: "+ resourcePath, e);
        }

        return new AppProperties(props);
    }


    @NonNull
    public <T> T getValue(String key){
        return getValue(key, "");
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, String defaultValue){
        return (T) properties.getOrDefault(key, defaultValue);
    }
}
