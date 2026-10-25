package ypfunding.server.service;

import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.*;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.exception.SoldOutException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.*;

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
        if(user.userType() != UserType.SUPPORTER)
            throw new NotAllowedException("서포터만 후원을 신청할 수 있습니다.");

        if(quantity < DomainRules.MIN_FUND_QUANTITY || quantity > DomainRules.MAX_FUND_QUANTITY)
            throw new BusinessException("수량을 " + DomainRules.MIN_FUND_QUANTITY + "개 이상 " +
                    DomainRules.MAX_FUND_QUANTITY + "개 이하로 선택해 주세요.");

        LocalDateTime now = LocalDateTime.now(clock);

        transactionManager.execute(conn ->{
            //리워드 잠금 후 확인
            RewardDTO reward = rewardsDAO.findByIdForUpdate(conn, rewardId);
            if(reward == null)
                throw new BusinessException("존재하지 않는 리워드입니다.");

            //프로젝트 확인
            ProjectDTO project = projectsDAO.findById(conn, reward.getProjectId());
            if (project == null) {
                throw new BusinessException("존재하지 않는 프로젝트입니다.");
            }
            //만약 projectDAO에서 진행중인지 확인하는 메서드가 구현될시 교체 예정
            boolean ongoing = project.getApprovalStatus() == ApprovalStatus.APPROVED
                    && !now.isBefore(project.getStartDate())
                    && now.isBefore(project.getEndDate());

            if (!ongoing) {
                throw new BusinessException("진행 중인 프로젝트에만 후원할 수 있습니다.");
            }

            //남은 수량 확인. stock = 제공 수량, 0 = 무제한
            long stock = reward.getStock();
            if (stock != 0) {
                long sold = fundsDAO.sumQuantityByReward(conn, rewardId);
                if (sold + quantity > stock) {
                    throw new SoldOutException();
                }
            }

            //후원 저장. 중복이면 DAO가 DuplicateFundException
            FundDTO fund = new FundDTO();
            fund.setUserId(user.userId());
            fund.setProjectId(reward.getProjectId());
            fund.setRewardId(rewardId);
            fund.setQuantity((long) quantity);
            fund.setDate(now);
            fundsDAO.insert(conn, fund);

            return null;
        });
    }

    //후원 취소
    public void cancelFund(LoginUser user, long fundId, String content){
        if(user.userType() != UserType.SUPPORTER)
            throw new NotAllowedException("서포터만 후원을 취소할 수 있습니다.");

        if(content == null || content.isBlank())
            throw new BusinessException("취소 사유를 입력해 주세요.");

        LocalDateTime now = LocalDateTime.now(clock);

        transactionManager.execute(conn ->{
            //후원 확인
            FundDTO fund = fundsDAO.findById(conn, fundId);
            if(fund == null){
                throw new BusinessException("존재하지 않는 후원입니다.");
            }

            if(fund.getUserId() != user.userId()){
                throw new NotAllowedException("본인의 후원만 취소할 수 있습니다.");
            }

            //reward잠금, 신청과 같은 잠금 순서를 지켜 데드락 방지
            rewardsDAO.findByIdForUpdate(conn, fund.getRewardId());

            //프로젝트 확인
            ProjectDTO project = projectsDAO.findById(conn, fund.getProjectId());
            if(project == null){
                throw new BusinessException("존재하지 않는 프로젝트입니다.");
            }

            //만약 projectDAO에서 진행중인지 확인하는 메서드가 구현될시 교체 예정
            boolean ongoing = project.getApprovalStatus() == ApprovalStatus.APPROVED
                    && !now.isBefore(project.getStartDate())
                    && now.isBefore(project.getEndDate());

            if (!ongoing) {
                throw new BusinessException("진행 중인 프로젝트의 후원만 취소 할 수 있습니다.");
            }

            //취소 기록 저장
            CanceledFundDTO canceled = new CanceledFundDTO();
            canceled.setUserId(fund.getUserId());
            canceled.setProjectId(fund.getProjectId());
            canceled.setRewardId(fund.getRewardId());
            canceled.setQuantity(fund.getQuantity());
            canceled.setContent(content);
            canceled.setCancelDate(now);
            canceledFundsDAO.insert(conn, canceled);

            //후원 삭제. 0일 경우 이미 취소됨
            if (fundsDAO.delete(conn, fundId) == 0){
                throw new BusinessException("이미 취소된 후원입니다.");
            }

            return null;
        });
    }

    //후원 내역 조회
    public List<FundQueryDTO> getMyFunds(LoginUser user) {
        if(user.userType() != UserType.SUPPORTER) {
            throw new NotAllowedException("서포터만 후원 내역을 볼 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        return transactionManager.execute(conn -> {
            List<FundQueryDTO> result = new ArrayList<>();
            result.addAll(fundQueryDAO.findMyFunds(conn, user.userId(), now));
            result.addAll(fundQueryDAO.findMyCanceledFunds(conn, user.userId()));
            result.sort(Comparator.comparing(FundQueryDTO::getDate).reversed());
            return result;
        });
    }
}
