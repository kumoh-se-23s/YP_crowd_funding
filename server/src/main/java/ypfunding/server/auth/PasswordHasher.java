package ypfunding.server.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

public class PasswordHasher {
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    //소문자 hex 변환
    private static final HexFormat HEX = HexFormat.of();

    //새 salt 생성
    public static String newSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    //해시
    public static String hash(String salt, String password) {
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(password, "password");
        try {
            //MessageDigest는 스레드 안전하지 않으니 매번 새로 제작
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hashed);
        } catch (NoSuchAlgorithmException e) { //모든 jvm이 지원하므로 사실상 작동될 일 없음
            throw new IllegalStateException("SHA-256을 사용할 수 없음", e);
        }
    }

    //맞는지 확인
    public static boolean matches(String salt, String password, String expectedHash) {
        Objects.requireNonNull(expectedHash, "expectedHash");
        byte[] actual = hash(salt, password).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
