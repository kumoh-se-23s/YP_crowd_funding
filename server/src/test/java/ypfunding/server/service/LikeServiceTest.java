package ypfunding.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LikeServiceTest extends DbTestBase {
    private static final int DURATION_DAYS = 30;

    private long insertMaker() {
        return tx.execute(conn -> TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));
    }

    private long insertSupporter(String loginId) {
        return tx.execute(conn -> TestData.insertUser(conn, loginId, UserType.SUPPORTER, clock.now()));
    }

    private long insertOngoingProject(long makerId) {
        return tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, 100000, DURATION_DAYS, clock.now()));
    }

    private int countLikes(long projectId) {
        return tx.execute(conn -> TestData.countLikes(conn, projectId));
    }

    @Test
    @DisplayName("진행 중인 프로젝트에 처음 누르면 좋아요가 되고, 다시 누르면 취소된다")
    void toggleOnAndOff() {
        long makerId = insertMaker();
        long projectId = insertOngoingProject(makerId);
        LoginUser supporter = new LoginUser(insertSupporter("sup"), UserType.SUPPORTER);
        LikeService likeService = context.getLikeService();

        assertTrue(likeService.toggle(supporter, projectId));
        assertEquals(1, countLikes(projectId));

        assertFalse(likeService.toggle(supporter, projectId));
        assertEquals(0, countLikes(projectId));
    }

    @Test
    @DisplayName("이미 좋아요한 상태에서 누르면 취소된다")
    void alreadyLikedThenCancel() {
        long makerId = insertMaker();
        long projectId = insertOngoingProject(makerId);
        long supporterId = insertSupporter("sup");
        tx.execute(conn -> {
            TestData.insertLike(conn, supporterId, projectId);
            return null;
        });

        boolean result = context.getLikeService()
                .toggle(new LoginUser(supporterId, UserType.SUPPORTER), projectId);

        assertFalse(result);
        assertEquals(0, countLikes(projectId));
    }

    @Test
    @DisplayName("여러 서포터의 좋아요는 각각 따로 센다")
    void multipleSupporters() {
        long makerId = insertMaker();
        long projectId = insertOngoingProject(makerId);
        LikeService likeService = context.getLikeService();

        likeService.toggle(new LoginUser(insertSupporter("sup1"), UserType.SUPPORTER), projectId);
        likeService.toggle(new LoginUser(insertSupporter("sup2"), UserType.SUPPORTER), projectId);

        assertEquals(2, countLikes(projectId));
    }

    @Test
    @DisplayName("메이커가 누르면 권한 없음으로 거부된다")
    void makerIsRejected() {
        long makerId = insertMaker();
        long projectId = insertOngoingProject(makerId);
        LoginUser maker = new LoginUser(makerId, UserType.MAKER);

        assertThrows(NotAllowedException.class,
                () -> context.getLikeService().toggle(maker, projectId));
        assertEquals(0, countLikes(projectId));
    }

    @Test
    @DisplayName("승인 대기 중인 프로젝트에는 누를 수 없다")
    void pendingProjectIsRejected() {
        long makerId = insertMaker();
        long projectId = tx.execute(conn -> TestData.insertPendingProject(conn, makerId, 100000, DURATION_DAYS));
        LoginUser supporter = new LoginUser(insertSupporter("sup"), UserType.SUPPORTER);

        assertThrows(BusinessException.class,
                () -> context.getLikeService().toggle(supporter, projectId));
        assertEquals(0, countLikes(projectId));
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트에는 누를 수 없다")
    void unknownProjectIsRejected() {
        LoginUser supporter = new LoginUser(insertSupporter("sup"), UserType.SUPPORTER);

        assertThrows(BusinessException.class,
                () -> context.getLikeService().toggle(supporter, 999999L));
    }

    @Test
    @DisplayName("종료 시각이 되면 마감되어 누를 수 없다")
    void atEndIsRejected() {
        long makerId = insertMaker();
        long projectId = insertOngoingProject(makerId);
        LoginUser supporter = new LoginUser(insertSupporter("sup"), UserType.SUPPORTER);

        clock.plus(Duration.ofDays(DURATION_DAYS));

        assertThrows(BusinessException.class,
                () -> context.getLikeService().toggle(supporter, projectId));
        assertEquals(0, countLikes(projectId));
    }
}
