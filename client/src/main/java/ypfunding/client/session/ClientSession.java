package ypfunding.client.session;

import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;

/**
 * 현재 CLI 클라이언트에 로그인한 사용자 정보(LoginUser 및 UserDTO)와 접속 상태를 보관하는 세션 클래스.
 */
public final class ClientSession {

    private static LoginUser currentLoginUser;
    private static UserDTO currentUserDto;

    //콘솔 단위에서 1인 1프로그램으로 제어하기 위해 생성자 private
    private ClientSession() {
    }

    /**
     * 로그인 성공 시 세션 정보를 등록
     *
     * @param loginUser 서버 인증 객체 (userId, userType)
     * @param userDto   화면 표시용 사용자 상세 정보 (이름, 아이디, 주소 등)
     */
    public static void login(LoginUser loginUser, UserDTO userDto) {
        currentLoginUser = loginUser;
        currentUserDto = userDto;
    }

    /**
     * 로그아웃 시 세션 정보를 초기화
     */
    public static void logout() {
        currentLoginUser = null;
        currentUserDto = null;
    }

    /**
     * 현재 로그인 여부를 반환
     *
     * @return 로그인 상태이면 true, 아니면 false
     */
    public static boolean isLoggedIn() {
        return currentLoginUser != null;
    }

    /**
     * 현재 로그인한 LoginUser를 반환
     *
     * @return 현재 LoginUser (비로그인 시 null)
     */
    public static LoginUser getLoginUser() {
        return currentLoginUser;
    }

    /**
     * 현재 로그인한 사용자의 UserDTO를 반환
     *
     * @return 현재 UserDTO (비로그인 시 null)
     */
    public static UserDTO getUserDto() {
        return currentUserDto;
    }

    /**
     * 개인정보 수정 후 세션에 보관된 UserDTO를 갱신
     *
     * @param updatedUserDto 갱신된 UserDTO
     */
    public static void updateUserDto(UserDTO updatedUserDto) {
        currentUserDto = updatedUserDto;
    }

    /**
     * 현재 로그인한 사용자의 역할(UserType)을 반환
     *
     * @return UserType (비로그인 시 null)
     */
    public static UserType getUserType() {
        return currentLoginUser != null ? currentLoginUser.userType() : null;
    }
}