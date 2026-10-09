package ypfunding.server.exception;

//db 문제 발생
public class DataAccessException extends RuntimeException{
    //sql 에러를 포함
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    //sql은 됐는데 뭔가 결과가 이상하면 이거 사용
    public DataAccessException(String message) {
        super(message);
    }
}
