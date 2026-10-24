package ypfunding.server.service;

import ypfunding.common.dto.LikeDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.LikesDAO;
import ypfunding.server.persistence.dao.ProjectsDAO;

import java.time.Clock;
import java.time.LocalDateTime;


public class LikeService extends Service{
    private final LikesDAO likesDAO;
    private final ProjectsDAO projectsDAO;
    private final Clock clock;

    public LikeService(TransactionManager transactionManager, LikesDAO likesDAO, ProjectsDAO projectsDAO, Clock clock) {
        super(transactionManager);
        this.likesDAO = likesDAO;
        this.projectsDAO = projectsDAO;
        this.clock = clock;
    }

    // =================================================================
    // 좋아요 토글 형식으로 처리
    // =================================================================

    // 실행 후 상태를 true or false로 알림

    public boolean toggle(LoginUser user, long projectId){
        if(user.userType() != UserType.SUPPORTER)
            throw new NotAllowedException("서포터만 좋아요를 누를 수 있습니다.");

        LocalDateTime now = LocalDateTime.now(clock);

        return transactionManager.execute(conn ->{
            ProjectDTO project = projectsDAO.findById(conn, projectId);

            if(project == null)
                throw new BusinessException("존재하지 않는 프로젝트입니다.");

            //만약 projectDAO에서 진행중인지 확인하는 메서드가 구현될시 교체 예정
            boolean ongoing = project.getApprovalStatus() == ApprovalStatus.APPROVED
                    && !now.isBefore(project.getStartDate())
                    && now.isBefore(project.getEndDate());

            if(!ongoing)
                throw new BusinessException("진행 중인 프로젝트에만 좋아요를 누를 수 있습니다.");

            int delete = likesDAO.delete(conn, user.userId(), projectId);
            if(delete == 1) return false;

            LikeDTO like = new LikeDTO();
            like.setUserID(user.userId());
            like.setProjectID(projectId);
            likesDAO.insert(conn, like);
            return true;
        });
    }
}
