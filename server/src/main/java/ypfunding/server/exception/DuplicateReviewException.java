package ypfunding.server.exception;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(Throwable cause) {
        super("이미 리뷰를 작성한 후원입니다.", cause);
    }
}
