package ypfunding.client.view;

import ypfunding.client.session.ClientSession;
import ypfunding.client.util.ConsoleIO;
import ypfunding.common.dto.UserDTO;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.service.AuthService;
import ypfunding.server.service.UserService;


// 비로그인 메뉴(로그인, 서포터 회원가입) 및 마이페이지 개인정보·비밀번호 수정 담당

public class AuthCliView {

    private final AuthService authService;
    private final UserService userService;

    public AuthCliView(AppContext context) {
        this.authService = context.getAuthService();
        this.userService = context.getUserService();
    }

    //비로그인 시작 메뉴를 출력하고 CLI running control 수행

    public boolean showAnonymousMenu() {
        System.out.println();
        ConsoleIO.printDoubleLine();
        System.out.println(" [ YP 크라우드펀딩 시스템 ]");
        ConsoleIO.printDoubleLine();
        System.out.println(" 1. 로그인");
        System.out.println(" 2. 회원가입 (서포터 전용)");
        System.out.println(" 3. [시연용] 제공 수량 동시성 제어 테스트 (평가표 19)");
        System.out.println(" 0. 프로그램 종료");
        ConsoleIO.printSingleLine();

        //switch 내부 default를 없애고 밖에서 0~3 범위로 강제 -> ConsoleIO에서 입력에 대한 예외 처리 위임
        int choice = ConsoleIO.readIntInRange("선택 > ", 0, 3);
        switch (choice) {
            case 1 -> handleLogin();
            case 2 -> handleSignup();
            case 3 -> handleConcurrencyDemo();
            case 0 -> {
                return false;
            }
        }
        return true;
    }
    // 로그인 기능
    private void handleLogin() {
        System.out.println();
        String loginId = ConsoleIO.readLine("• 아이디 : ");
        String password = ConsoleIO.readLine("• 비밀번호 : ");

        try {
            LoginUser loginUser = authService.login(loginId, password);
            UserDTO userDto = userService.getMyInfo(loginUser);
            ClientSession.login(loginUser, userDto);
            System.out.printf("=> [로그인 성공] %s님(%s), 환영합니다!%n",
                    userDto.getName(), userDto.getUserType().name());
        } catch (BusinessException e) {
            ConsoleIO.printlnError("아이디 또는 비밀번호가 올바르지 않습니다.");
        } catch (Exception e) {
            ConsoleIO.printlnError("로그인 처리 중 서버 오류가 발생했습니다.");
        }
    }

    //서포터 회원가입
    private void handleSignup() {
        System.out.println();
        System.out.println("---------------- [서포터 회원가입] ----------------");
        String loginId = ConsoleIO.readNonBlankString("• 아이디 (최대 30자) : ");
        String password = ConsoleIO.readNonBlankString("• 비밀번호 : ");
        String name = ConsoleIO.readNonBlankString("• 이름 (최대 30자) : ");
        String address = ConsoleIO.readNonBlankString("• 주소(배송지) : ");

        try {
            long userId = authService.signup(loginId, password, name, address);
            ConsoleIO.printlnSuccess("회원가입이 완료되었습니다! (생성된 회원 번호: #" + userId + ")");
        } catch (BusinessException e) {
            ConsoleIO.printlnError(e.getMessage());
        } catch (Exception e) {
            ConsoleIO.printlnError("회원가입 처리 중 오류가 발생했습니다.");
        }
    }

    //임시 출력 예제
    private void handleConcurrencyDemo() {
        System.out.println("3번 입력 정상 출력 확인 용도 ");
    }

    // 개인정보 및 비밀번호 수정 메뉴를 출력하고 처리
    public void showProfileUpdateMenu() {
        while (true) {
            System.out.println();
            System.out.println("---------------- [개인정보 수정] ----------------");
            UserDTO current = ClientSession.getUserDto();
            if (current != null) {
                System.out.printf(" [현재 정보] 이름: %s | 주소: %s%n",
                        current.getName(), current.getAddress());
                ConsoleIO.printSingleLine();
            }
            System.out.println(" 1. 이름 및 주소(배송지) 변경");
            System.out.println(" 2. 비밀번호 변경 (현재 비밀번호 확인 필수)");
            System.out.println(" 0. 뒤로가기");

            int choice = ConsoleIO.readIntInRange("선택 > ", 0, 2);
            switch (choice) {
                case 1 -> updateNameAndAddress();
                case 2 -> updatePassword();
                case 0 -> {
                    return;
                }
            }
        }
    }

    //이름 및 주소를 변경 후 ClientSession 정보를 최신화=
    private void updateNameAndAddress() {
        LoginUser loginUser = ClientSession.getLoginUser();
        String newName = ConsoleIO.readNonBlankString("• 새 이름 입력 : ");
        String newAddress = ConsoleIO.readNonBlankString("• 새 주소 입력 : ");

        try {
            userService.updateProfile(loginUser, newName, newAddress);
            UserDTO refreshed = userService.getMyInfo(loginUser);
            ClientSession.updateUserDto(refreshed);
            ConsoleIO.printlnSuccess("개인정보가 수정되었습니다.");
        } catch (BusinessException e) {
            ConsoleIO.printlnError(e.getMessage());
        } catch (Exception e) {
            ConsoleIO.printlnError("개인정보 수정 중 오류가 발생했습니다.");
        }
    }

    // 현재 비밀번호를 확인 후 새 비밀번호로 변경
    private void updatePassword() {
        LoginUser loginUser = ClientSession.getLoginUser();
        String currentPassword = ConsoleIO.readNonBlankString("• 현재 비밀번호 입력 : ");
        String newPassword = ConsoleIO.readNonBlankString("• 새 비밀번호 입력 : ");

        try {
            authService.changePassword(loginUser, currentPassword, newPassword);
            ConsoleIO.printlnSuccess("비밀번호가 변경되었습니다.");
        } catch (BusinessException e) {
            ConsoleIO.printlnError(e.getMessage());
        } catch (Exception e) {
            ConsoleIO.printlnError("비밀번호 변경 중 오류가 발생했습니다.");
        }
    }
}