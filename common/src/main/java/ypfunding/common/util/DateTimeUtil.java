package ypfunding.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

//시간 관련 공용 함수 모음
public class DateTimeUtil {
    //어느 환경에서든 KST 기준으로 동작하는 zone
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    //화면 표시용 날짜+시간 형식
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    //화면 표시용 날짜 형식
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //target이 now보다 이전인지 반환
    public static boolean isPast(LocalDateTime target, LocalDateTime now) {
        return target.isBefore(now);
    }

    //날짜+시간을 화면표시용으로 변환
    public static String formatDateTime(LocalDateTime dateTime) {
        if(dateTime == null)
            return "---- -- -- -- --";
        else
            return dateTime.format(DISPLAY_DATETIME_FORMAT);
    }

    //날짜를 화면표시용으로 변환
    public static String formatDate(LocalDateTime dateTime) {
        if(dateTime == null)
            return "---- -- --";
        else
            return dateTime.format(DISPLAY_DATE_FORMAT);
    }
}
