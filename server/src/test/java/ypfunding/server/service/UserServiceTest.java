package ypfunding.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

//내 정보 조회·수정 (평가표 10 / DB 검사 5 "(MyPage) 개인정보 수정")
class UserServiceTest extends DbTestBase {

    @Test
    @DisplayName("내 정보를 조회하면 저장된 값을 받는다")
    void getMyInfoReturnsStoredValues() {
        long userId = tx.execute(conn -> TestData.insertUser(conn, "sup1", UserType.SUPPORTER, clock.now()));

        UserDTO me = context.getUserService().getMyInfo(new LoginUser(userId, UserType.SUPPORTER));

        assertEquals(userId, me.getUserId());
        assertEquals("sup1", me.getLoginId());
        assertEquals("sup1 이름", me.getName());
        assertEquals("sup1 주소", me.getAddress());
        assertEquals(UserType.SUPPORTER, me.getUserType());
        assertEquals(BASE_TIME, me.getRegDate());
    }

    @Test
    @DisplayName("이름과 주소를 바꾸면 반영되고, 아이디는 그대로다")
    void updateProfileChangesNameAndAddressOnly() {
        long userId = tx.execute(conn -> TestData.insertUser(conn, "sup1", UserType.SUPPORTER, clock.now()));
        LoginUser user = new LoginUser(userId, UserType.SUPPORTER);
        UserService service = context.getUserService();

        service.updateProfile(user, "새이름", "부산시 해운대구");

        UserDTO me = service.getMyInfo(user);
        assertEquals("새이름", me.getName());
        assertEquals("부산시 해운대구", me.getAddress());
        assertEquals("sup1", me.getLoginId());
    }

    @Test
    @DisplayName("메이커와 관리자도 자기 정보를 바꿀 수 있다")
    void makerAndAdminCanUpdateProfile() {
        UserService service = context.getUserService();
        for (UserType type : new UserType[]{UserType.MAKER, UserType.ADMIN}) {
            long userId = tx.execute(conn -> TestData.insertUser(conn, type.name(), type, clock.now()));
            LoginUser user = new LoginUser(userId, type);

            service.updateProfile(user, "새이름", "새주소");

            assertEquals("새이름", service.getMyInfo(user).getName());
        }
    }

    @Test
    @DisplayName("빈 값이나 너무 긴 값이면 바뀌지 않는다")
    void updateProfileRejectsBlankOrTooLongInput() {
        long userId = tx.execute(conn -> TestData.insertUser(conn, "sup1", UserType.SUPPORTER, clock.now()));
        LoginUser user = new LoginUser(userId, UserType.SUPPORTER);
        UserService service = context.getUserService();

        assertThrows(BusinessException.class, () -> service.updateProfile(user, "", "새주소"));
        assertThrows(BusinessException.class, () -> service.updateProfile(user, "새이름", "   "));
        assertThrows(BusinessException.class, () -> service.updateProfile(user, "가".repeat(31), "새주소"));
        assertThrows(BusinessException.class, () -> service.updateProfile(user, "새이름", "가".repeat(201)));

        UserDTO me = service.getMyInfo(user);
        assertEquals("sup1 이름", me.getName());
        assertEquals("sup1 주소", me.getAddress());
    }

    @Test
    @DisplayName("없는 사용자면 예외")
    void unknownUserFails() {
        LoginUser ghost = new LoginUser(999_999, UserType.SUPPORTER);
        UserService service = context.getUserService();

        assertThrows(BusinessException.class, () -> service.getMyInfo(ghost));
        assertThrows(BusinessException.class, () -> service.updateProfile(ghost, "새이름", "새주소"));
    }
}