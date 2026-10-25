package ypfunding.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.SoldOutException;
import ypfunding.server.support.Concurrent;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FundServiceTest extends DbTestBase {

    @Test
    @DisplayName("제공 수량 1개에 20명이 동시에 신청하면 1명만 성공하고 19명은 품절")
    void concurrentSoldOut() {
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));
        long projectId = tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, 100000, 30, clock.now()));
        long rewardId = tx.execute(conn -> TestData.insertReward(conn, projectId, 10000, 1));   // 제공 수량 1

        List<LoginUser> users = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String loginId = "sup" + i;
            long id = tx.execute(conn -> TestData.insertUser(conn, loginId, UserType.SUPPORTER, clock.now()));
            users.add(new LoginUser(id, UserType.SUPPORTER));
        }

        List<Throwable> results = Concurrent.runAtOnce(20,
                i -> context.getFundService().applyFund(users.get(i), rewardId, 1));

        long sold = tx.execute(conn -> TestData.sumFundQuantity(conn, rewardId));
        System.out.println("성공: " + Concurrent.successCount(results) + ", 팔린 수량: " + sold);

        assertEquals(1, Concurrent.successCount(results));
        assertEquals(19, Concurrent.countOf(results, SoldOutException.class));
        assertEquals(1, sold);
    }
}