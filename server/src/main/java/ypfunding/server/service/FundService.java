package ypfunding.server.service;

import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.*;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.PermissionDeniedException;
import ypfunding.server.exception.SoldOutException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.*;
import ypfunding.server.util.ProjectStatusUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FundService extends Service{
    private final FundsDAO fundsDAO;
    private final CanceledFundsDAO canceledFundsDAO;
    private final RewardsDAO rewardsDAO;
    private final ProjectsDAO projectsDAO;
    private final Clock clock;
    private final FundQueryDAO fundQueryDAO;

    public FundService(TransactionManager transactionManager, FundsDAO fundsDAO, CanceledFundsDAO canceledFundsDAO,
                       RewardsDAO rewardsDAO, ProjectsDAO projectsDAO, FundQueryDAO fundQueryDAO, Clock clock){
        super(transactionManager);
        this.fundsDAO = fundsDAO;
        this.canceledFundsDAO = canceledFundsDAO;
        this.rewardsDAO = rewardsDAO;
        this.projectsDAO = projectsDAO;
        this.fundQueryDAO = fundQueryDAO;
        this.clock = clock;
    }

    //후원 신청
    public void applyFund(LoginUser user, long rewardId, int quantity){
        requireSupporter(user, "서포터만 후원을 신청할 수 있습니다.");
        requireValidQuantity(quantity);
        LocalDateTime now = LocalDateTime.now(clock);

        transactionManager.execute(conn ->{
            //리워드 잠금 후 확인
            RewardDTO reward = lockReward(conn, rewardId);
            //프로젝트 확인
            requireOngoingProject(conn, reward.getProjectId(), now, "진행 중인 프로젝트에만 후원할 수 있습니다.");
            requireEnoughRemaining(conn, reward, quantity);
            fundsDAO.insert(conn, newFund(user.userId(), reward, quantity, now));
            return null;
        });
    }

    //후원 취소
    public void cancelFund(LoginUser user, long fundId, String content){
        requireSupporter(user, "서포터만 후원을 취소 할 수 있습니다.");
        requireCancelReason(content);
        LocalDateTime now = LocalDateTime.now(clock);

        transactionManager.execute(conn ->{
            //후원 확인
            FundDTO fund = findOwnFund(conn, user, fundId);
            //reward잠금, 신청과 같은 잠금 순서를 지켜 데드락 방지
            rewardsDAO.findByIdForUpdate(conn, fund.getRewardId());

            requireOngoingProject(conn, fund.getProjectId(), now, "진행 중인 프로젝트의 후원만 취소 할 수 있습니다.");
            canceledFundsDAO.insert(conn, newCanceledFund(fund, content, now));
            //후원 삭제. 0일 경우 이미 취소됨
            if (fundsDAO.delete(conn, fundId) == 0){
                throw new BusinessException("이미 취소된 후원입니다.");
            }
            return null;
        });
    }

    //후원 내역 조회
    public List<FundQueryDTO> getMyFunds(LoginUser user) {
        requireSupporter(user, "서포터만 후원 내역을 볼 수 있습니다.");
        LocalDateTime now = LocalDateTime.now(clock);

        return transactionManager.execute(conn -> {
            List<FundQueryDTO> result = new ArrayList<>();
            result.addAll(fundQueryDAO.findMyFunds(conn, user.userId(), now));
            result.addAll(fundQueryDAO.findMyCanceledFunds(conn, user.userId()));
            result.sort(Comparator.comparing(FundQueryDTO::getDate).reversed());
            return result;
        });
    }

    // ----- 트랜잭션 밖 검사 -----

    private static void requireSupporter(LoginUser user, String message) {
        if (user.userType() != UserType.SUPPORTER) {
            throw new PermissionDeniedException(message);
        }
    }

    private static void requireValidQuantity(int quantity) {
        if (quantity < DomainRules.MIN_FUND_QUANTITY || quantity > DomainRules.MAX_FUND_QUANTITY) {
            throw new BusinessException("수량은 " + DomainRules.MIN_FUND_QUANTITY + "개 이상 "
                    + DomainRules.MAX_FUND_QUANTITY + "개 이하로 선택해 주세요.");
        }
    }

    private static void requireCancelReason(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("취소 사유를 입력해 주세요.");
        }
    }

    // ----- 트랜잭션 안 검사 -----
    //리워드 행을 잠그고 조회한다. 반드시 트랜잭션의 첫 SQL이어야 한다.
    private RewardDTO lockReward(Connection conn, long rewardId) throws SQLException {
        RewardDTO reward = rewardsDAO.findByIdForUpdate(conn, rewardId);
        if (reward == null) {
            throw new BusinessException("존재하지 않는 리워드입니다.");
        }
        return reward;
    }

    private void requireOngoingProject(Connection conn, long projectId, LocalDateTime now, String message)
            throws SQLException {
        ProjectDTO project = projectsDAO.findById(conn, projectId);
        if (project == null) {
            throw new BusinessException("존재하지 않는 프로젝트입니다.");
        }
        if (!ProjectStatusUtil.isOngoing(project, now)) {
            throw new BusinessException(message);
        }
    }

    // 남은 수량 확인. stock = 제공 수량(고정), 0 = 무제한
    private void requireEnoughRemaining(Connection conn, RewardDTO reward, int quantity) throws SQLException {
        long stock = reward.getStock();
        if (stock == 0) {
            return;
        }
        long sold = fundsDAO.sumQuantityByReward(conn, reward.getRewardId());
        if (sold + quantity > stock) {
            throw new SoldOutException();
        }
    }

    private FundDTO findOwnFund(Connection conn, LoginUser user, long fundId) throws SQLException {
        FundDTO fund = fundsDAO.findById(conn, fundId);
        if (fund == null) {
            throw new BusinessException("존재하지 않는 후원입니다.");
        }
        if (fund.getUserId() != user.userId()) {
            throw new PermissionDeniedException("본인의 후원만 취소할 수 있습니다.");
        }
        return fund;
    }

    // ----- DTO 만들기 -----
    private static FundDTO newFund(long userId, RewardDTO reward, int quantity, LocalDateTime now) {
        FundDTO fund = new FundDTO();
        fund.setUserId(userId);
        fund.setProjectId(reward.getProjectId());
        fund.setRewardId(reward.getRewardId());
        fund.setQuantity((long) quantity);
        fund.setDate(now);
        return fund;
    }

    private static CanceledFundDTO newCanceledFund(FundDTO fund, String content, LocalDateTime now) {
        CanceledFundDTO canceled = new CanceledFundDTO();
        canceled.setUserId(fund.getUserId());
        canceled.setProjectId(fund.getProjectId());
        canceled.setRewardId(fund.getRewardId());
        canceled.setQuantity(fund.getQuantity());
        canceled.setContent(content);
        canceled.setCancelDate(now);
        return canceled;
    }
}
