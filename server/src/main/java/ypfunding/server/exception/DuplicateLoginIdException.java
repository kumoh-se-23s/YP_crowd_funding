package ypfunding.server.exception;

//회원가입 시 이미 있는 아이디
public class DuplicateLoginIdException extends BusinessException {
    public DuplicateLoginIdException(Throwable cause) {
        super("이미 사용 중인 아이디입니다.", cause);
    }
}