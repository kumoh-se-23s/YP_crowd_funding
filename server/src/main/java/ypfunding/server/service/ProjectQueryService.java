package ypfunding.server.service;

import ypfunding.common.dto.ProjectDTO;
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

    public List<ProjectDTO> getAllProjects(long userID){
        return transactionManager.execute(conn ->{
            if(userService.checkNotAdmin(conn, userID)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
            }
            return projectsDAO.getDisplayedProjectsWithFundingStatus(conn);
        });
    }

}
