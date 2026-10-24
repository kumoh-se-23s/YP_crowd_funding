package ypfunding.server.service;

import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.FundDTO;
import ypfunding.common.dto.LikeDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.exception.SoldOutException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.CanceledFundsDAO;
import ypfunding.server.persistence.dao.FundsDAO;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.RewardsDAO;

import java.time.Clock;
import java.time.LocalDateTime;

public class FundService extends Service{
    private final FundsDAO fundsDAO;
    private final CanceledFundsDAO canceledFundsDAO;
    private final RewardsDAO rewardsDAO;
    private final ProjectsDAO projectsDAO;
    private final Clock clock;

    public FundService(TransactionManager transactionManager, FundsDAO fundsDAO, CanceledFundsDAO canceledFundsDAO,
                       RewardsDAO rewardsDAO, ProjectsDAO projectsDAO, Clock clock){
        super(transactionManager);
        this.fundsDAO = fundsDAO;
        this.canceledFundsDAO = canceledFundsDAO;
        this.rewardsDAO = rewardsDAO;
        this.projectsDAO = projectsDAO;
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
            //리워드 확인
            RewardDTO reward = rewardsDAO.findById(conn, rewardId);
            if(reward == null)
                throw new BusinessException("존재하지 않는 리워드입니다.");

            //프로젝트 확인
            ProjectDTO project = projectsDAO.findById(conn, reward.getProjectId());
            //만약 projectDAO에서 진행중인지 확인하는 메서드가 구현될시 교체 예정
            boolean ongoing = project.getApprovalStatus() == ApprovalStatus.APPROVED
                    && !now.isBefore(project.getStartDate())
                    && now.isBefore(project.getEndDate());

            if (project == null || !ongoing) {
                throw new BusinessException("진행 중인 프로젝트에만 후원할 수 있습니다.");
            }

            //재고 차감
            if (rewardsDAO.decreaseStock(conn, rewardId, quantity) == 0) {
                throw new SoldOutException();
            }

            //후원 저장. 중복이면 DAO가 DuplicateFundException
            FundDTO fund = new FundDTO();
            fund.setUserId(user.userId());
            fund.setProjectId(reward.getProjectId());
            fund.setRewardId(rewardId);
            fund.setQuantity((long)quantity);
            fund.setDate(now);
            fundsDAO.insert(conn, fund);

            return null;
        });
    }

    //후원 취소
}
