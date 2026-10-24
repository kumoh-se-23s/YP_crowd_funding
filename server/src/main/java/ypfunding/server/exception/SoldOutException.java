package ypfunding.server.exception;

public class SoldOutException extends RuntimeException {
    public SoldOutException() {
        super("남은 수량이 부족합니다.");
    }
}
