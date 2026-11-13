package ypfunding.client.util;

import ypfunding.common.type.Category;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Scanner;

/**
 * 콘솔 입출력 검증(숫자, 문자열, Y/N, 파일 경로, 카테고리 선택) 및 포맷팅을 담당하는 유틸리티 클래스.
 */
public final class ConsoleIO {
    //모든 Scanner 객체는 ConsoleIO 아래에서 관리
    private static final Scanner SCANNER = new Scanner(System.in);

    //유틸 클래스 -> 생성자 호출 방지
    private ConsoleIO() {
    }

    /**
     * 프롬프트를 출력 후 한 라인의 문자열 입력받음
     *
     * @param prompt 안내 문구
     * @return 입력받은 문자열
     */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SCANNER.hasNextLine()) {
            return "";
        }
        return SCANNER.nextLine().trim();
    }

    /**
     * 빈 문자열을 허용 X 문자열을 입력받음
     *
     * @param prompt 안내 문구
     * @return 비어있지 않은 문자열
     */
    public static String readNonBlankString(String prompt) {
        while (true) {
            String input = readLine(prompt);
            if (!input.isBlank()) {
                return input;
            }
            printlnError("값을 비워둘 수 없습니다. 다시 입력해 주세요.");
        }
    }

    /**
     * int 입력 받는 메소드 int가 아니면 재입력을 요구
     *
     * @param prompt 안내 문구
     * @return 입력받은 정수
     */
    public static int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                printlnError("유효한 숫자를 입력해 주세요.");
            }
        }
    }

    /**
     * 지정된 범위(min ~ max) 내의 int 입력 받기.
     *
     * @param prompt 안내 문구
     * @param min    최소값 (포함)
     * @param max    최대값 (포함)
     * @return 범위 내의 정수
     */
    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (min <= value && value <= max) {
                return value;
            }
            printlnError(String.format("%d ~ %d 사이의 번호를 입력해 주세요.", min, max));
        }
    }

    /**
     * long 입력 받기
     *
     * @param prompt 안내 문구
     * @return 입력받은 long 값
     */
    public static long readLong(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                printlnError("유효한 숫자를 입력해 주세요.");
            }
        }
    }

    /**
     * 지정된 범위(min ~ ) 내의 long 입력 받기
     *
     * @param prompt 안내 문구
     * @param min    최소값 (포함)
     * @return min 이상의 long 값
     */
    public static long readLongMin(String prompt, long min) {
        while (true) {
            long value = readLong(prompt);
            if (value >= min) {
                return value;
            }
            printlnError(min + " 이상의 숫자를 입력해 주세요.");
        }
    }

    /**
     * y , n을 입력받아 boolean으로 반환
     *
     * @param prompt 안내 문구
     * @return y/yes이면 true, n/no이면 false
     */
    public static boolean readYesNo(String prompt) {
        while (true) {
            String input = readLine(prompt).toLowerCase(Locale.ROOT);
            if (input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            printlnError("'y' 또는 'n'을 입력해 주세요.");
        }
    }

    /**
     * 로컬 파일/디렉토리 경로를 입력받아 Path 객체로 반환
     *
     * @param prompt     안내 문구
     * @param allowBlank 엔터(빈 값) 허용 여부
     * @return 입력받은 Path (allowBlank가 true이고 빈 값이면 null)
     */
    public static Path readPath(String prompt, boolean allowBlank) {
        while (true) {
            String input = readLine(prompt);
            if (input.isBlank()) {
                if (allowBlank) {
                    return null;
                }
                printlnError("경로를 입력해 주세요.");
                continue;
            }
            // 따옴표로 감싸서 붙여넣은 경우 제거
            String cleaned = input.replace("\"", "").replace("'", "").trim();
            try {
                return Paths.get(cleaned);
            } catch (InvalidPathException e) {
                printlnError("올바르지 않은 경로 형식입니다: " + e.getMessage());
            }
        }
    }

    /**
     * 카테고리 선택 후 enum 반환
     *
     * @return 선택된 Category enum
     */
    public static Category selectSingleCategory() {
        Category[] categories = Category.values();
        System.out.println("• 카테고리 목록 :");
        for (int i = 0; i < categories.length; i++) {
            System.out.printf(" [%d] %-8s", i + 1, categories[i].getDisplayName());
            if ((i + 1) % 6 == 0 || i == categories.length - 1) {
                System.out.println();
            }
        }
        int choice = readIntInRange("• 카테고리 번호 선택 (1~" + categories.length + " 중 1개) : ", 1, categories.length);
        return categories[choice - 1];
    }

    /**
     * 원 단위로 포매팅
     *
     * @param amount 금액
     * @return 포맷된 문자열 (예: "500,000원")
     */
    public static String formatWon(Long amount) {
        if (amount == null) {
            return "0원";
        }
        return String.format(Locale.KOREA, "%,d원", amount);
    }

    /**
     * 구분선(60자 '=')을 출력
     */
    public static void printDoubleLine() {
        System.out.println("============================================================");
    }

    /**
     * 구분선(60자 '-')을 출력
     */
    public static void printSingleLine() {
        System.out.println("------------------------------------------------------------");
    }

    public static void printlnSuccess(String message) {
        System.out.println("=> [완료] " + message);
    }

    public static void printlnError(String message) {
        System.out.println("=> [실패] " + message);
    }
}