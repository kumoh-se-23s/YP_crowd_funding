package ypfunding.server.bootstrap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import ypfunding.common.type.UserType;

import java.time.LocalDateTime;

/**
 * MyBatis 설정 검증 전용 매퍼 (테스트 코드에만 있다).
 *
 * <p>운영 코드의 매퍼는 동적 쿼리라서 @SelectProvider를 쓰지만(설계 9.8),
 * 여기서는 설정(커넥션 처리, 이름 변환, 타입 변환)만 확인하면 되므로 고정 SQL(@Select)로 충분하다.
 */
public interface TestUserMapper {

    @Select("SELECT login_id FROM users WHERE user_id = #{userId}")
    String findLoginId(@Param("userId") long userId);

    @Select("SELECT name FROM users WHERE user_id = #{userId}")
    String findName(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM users")
    int countUsers();

    @Update("UPDATE users SET name = #{name} WHERE user_id = #{userId}")
    int rename(@Param("userId") long userId, @Param("name") String name);

    @Select("SELECT user_id, login_id, usertype, regdate FROM users WHERE user_id = #{userId}")
    UserRow findRow(@Param("userId") long userId);

    /**
     * 조회 결과를 담는 객체. 컬럼 user_id → 필드 userId 처럼 자동 연결되는지 확인한다.
     * (mapUnderscoreToCamelCase가 꺼져 있으면 userId, loginId가 null로 남는다)
     */
    class UserRow {
        private Long userId;
        private String loginId;
        private UserType usertype;
        private LocalDateTime regdate;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getLoginId() { return loginId; }
        public void setLoginId(String loginId) { this.loginId = loginId; }
        public UserType getUsertype() { return usertype; }
        public void setUsertype(UserType usertype) { this.usertype = usertype; }
        public LocalDateTime getRegdate() { return regdate; }
        public void setRegdate(LocalDateTime regdate) { this.regdate = regdate; }
    }
}