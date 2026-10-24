package ypfunding.server.service;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.notifications.NotificationType;
import ypfunding.common.notifications.PermissionDeniedNotification;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.*;
import ypfunding.server.util.NotificationUtils;

import java.util.List;

public class ProjectQueryService extends Service{

    private final UserService userService;
    private final ProjectQueryDAO projectQueryDAO;

    public ProjectQueryService(TransactionManager transactionManager, UserService userService, RewardsDAO rewardsDAO, FundsDAO fundsDAO) {
        super(transactionManager);
        this.userService = userService;
        this.projectQueryDAO = new ProjectQueryDAO(rewardsDAO, fundsDAO);
    }

    public List<ProjectQueryDTO> getSelectedProjectWithStatus(long userId, long projectId){
        return transactionManager.execute(conn ->{
            if(userService.checkNotAdmin(conn, userId)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
            }
            return projectQueryDAO.getProjectWithFundingStatus(conn, projectId);
        });
    }

    public List<ProjectQueryDTO> getAllProjectsWithStatus(long userId){
        return transactionManager.execute(conn ->{
            if(userService.checkNotAdmin(conn, userId)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
            }
            return projectQueryDAO.getDisplayedProjectsWithFundingStatus(conn);
        });
    }

}
