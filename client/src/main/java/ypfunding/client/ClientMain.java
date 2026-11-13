package ypfunding.client;

import ypfunding.client.session.ClientSession;
import ypfunding.client.util.ConsoleIO;
import ypfunding.common.type.UserType;
import ypfunding.server.bootstrap.AppContext;

/**
 * 크라우드펀딩 2차 CLI 프로그램의 메인 진입점.
 * AppContext를 초기화하고 로그인 상태 및 사용자 역할(MAKER, ADMIN, SUPPORTER)에 따라 화면을 분기
 */
public class ClientMain {

    public static void main(String[] args) {
        try (AppContext context = new AppContext()) {

            boolean running = true;
            while (running) {
                if (!ClientSession.isLoggedIn()) {
                    // before login CLI ( also control running )
                } else {
                    UserType role = ClientSession.getUserType();
                    if (role == UserType.MAKER) {
                        // maker CLI
                    } else if (role == UserType.ADMIN) {
                        // admin CLI
                    } else if (role == UserType.SUPPORTER) {
                        // supporter CLI
                    }
                }
            }
            System.out.println("프로그램을 종료합니다. 이용해 주셔서 감사합니다.");
        } catch (Exception e) {
            ConsoleIO.printlnError("시스템 초기화 또는 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

}