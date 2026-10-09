package ypfunding.server.exception;

//사용자가 관리자기능에 접근 시도하는 등 권한없는 요청 막기
public class NotAllowedException extends BusinessException{
    public NotAllowedException(String message) {
        super(message);
    }
}
