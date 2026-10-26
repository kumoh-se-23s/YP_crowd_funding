package ypfunding.server.util;

import ypfunding.server.exception.BusinessException;

//사용자 입력 문자열 검사
public class InputValidator {
    //빈 값 거부
    public static void requireNotBlank(String value, String label) {
        if(value == null || value.isBlank())
            throw new BusinessException(label + "을(를) 입력해 주세요.");
    }

    //빈 값 거부 및 최대 길이 검사
    public static void requireText(String value, String label, int maxLength) {
        requireNotBlank(value, label);
        if(value.length() > maxLength)
            throw new BusinessException(label + "은(는)" + maxLength + "자 이하로 입력되어야 합니다.");
    }
}
