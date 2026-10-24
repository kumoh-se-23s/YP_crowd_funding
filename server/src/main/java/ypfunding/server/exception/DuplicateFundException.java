package ypfunding.server.exception;

public class DuplicateFundException extends BusinessException{
    public DuplicateFundException(Throwable cause){
        super("이미 후원한 프로젝트입니다.", cause);
    }
}
