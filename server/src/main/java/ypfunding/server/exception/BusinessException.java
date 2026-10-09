package ypfunding.server.exception;

//사용자 차원의 문제를 다루는 예외
//이거 상속해서 품절, 중복후원, 후원없이 리뷰 등 각각 만들어서 핸들링하면 됨
public class BusinessException extends RuntimeException{
    public BusinessException(String message) {
        super(message);
    }

    //sql 에러코드 등 자세한 원인을 포함: cause는 오직 로그용
    //중복후원 등 다른 예외를 잡아서 변환할 때 사용
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
