package ypfunding.client;

import ypfunding.client.network.ClientNetworkConnector;
import ypfunding.client.network.ClientTCPConnector;
import ypfunding.client.util.ConsoleIO;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.request.RequestFundPacket;
import ypfunding.common.protocol.packets.request.RequestLoginPacket;
import ypfunding.common.protocol.packets.response.ResponseLoginPacket;
import ypfunding.common.protocol.packets.result.ResultFailedPacket;
import ypfunding.common.protocol.packets.result.ResultSuccessPacket;

import java.util.concurrent.TimeoutException;

/**
 * [시연용] RESPONSE 예시(로그인)와 RESULT 예시(후원 신청) 두 기능만 네트워크로 동작시키는 클라이언트.
 * 접속(TCP + TLS) → 로그인 → 후원 신청 반복.
 */
public class NetworkTestMain {

    static void main() {
        try (ClientNetworkConnector connector = new ClientTCPConnector()) {
            System.out.println("서버에 접속 중...");
            connector.start();
            System.out.println("접속 완료");

            UserDTO user = login(connector);
            if (user == null) {
                return;
            }

            while (true) {
                ConsoleIO.printSingleLine();
                System.out.println("1. 후원 신청   0. 종료");
                int menu = ConsoleIO.readIntInRange("• 선택 : ", 0, 1);
                if (menu == 0) {
                    break;
                }
                fund(connector);
            }
        } catch (TimeoutException e) {
            ConsoleIO.printlnError("서버 응답 시간이 초과되었습니다.");
        } catch (Exception e) {
            ConsoleIO.printlnError("통신 오류: " + e.getMessage());
        }
    }

    // 로그인 : 성공하면 RESPONSE(사용자 정보), 실패하면 RESULT FAILED(사유)
    private static UserDTO login(ClientNetworkConnector connector) throws TimeoutException {
        while (true) {
            String id = ConsoleIO.readLine("• 아이디 (빈 값이면 종료) : ");
            if (id.isBlank()) {
                return null;
            }
            RequestLoginPacket request = new RequestLoginPacket();
            request.setId(id);
            request.setPassword(ConsoleIO.readLine("• 비밀번호 : "));

            Packet reply = connector.request(request);
            if (reply instanceof ResponseLoginPacket response) {
                UserDTO user = response.getUser();
                ConsoleIO.printlnSuccess(user.getName() + "님 로그인 (" + user.getUserType() + ")");
                return user;
            }
            printFailure(reply);
        }
    }

    // 후원 신청 : 성공하면 RESULT SUCCESS(바디 없음), 실패하면 RESULT FAILED(사유)
    private static void fund(ClientNetworkConnector connector) throws TimeoutException {
        RequestFundPacket request = new RequestFundPacket();
        request.setRewardId(ConsoleIO.readLong("• 리워드 번호 : "));
        request.setQuantity(ConsoleIO.readInt("• 수량 : "));

        Packet reply = connector.request(request);
        if (reply instanceof ResultSuccessPacket) {
            ConsoleIO.printlnSuccess("후원이 완료되었습니다.");
            return;
        }
        printFailure(reply);
    }

    private static void printFailure(Packet reply) {
        if (reply instanceof ResultFailedPacket failed) {
            ConsoleIO.printlnError(failed.getReason());
        } else {
            ConsoleIO.printlnError("예상하지 못한 응답: " + reply.getClass().getSimpleName());
        }
    }
}