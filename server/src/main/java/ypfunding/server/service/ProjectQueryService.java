package ypfunding.server.service;

import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.notifications.PermissionDeniedNotification;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.*;
import ypfunding.server.util.NotificationUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProjectQueryService extends Service{

    private final UserService userService;
    private final ProjectQueryDAO projectQueryDAO;

    public ProjectQueryService(TransactionManager transactionManager, UserService userService, RewardsDAO rewardsDAO, FundsDAO fundsDAO) {
        super(transactionManager);
        this.userService = userService;
        this.projectQueryDAO = new ProjectQueryDAO(rewardsDAO, fundsDAO);
    }

    public List<ProjectQueryDTO> getSelectedProjectWithStatus(LoginUser user, long projectId){
        return transactionManager.execute(conn ->{
            if(userService.checkNotAdmin(conn, user)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return List.of();
            }
            return projectQueryDAO.getProject(conn, projectId);
        });
    }

    public List<ProjectQueryDTO> getAllProjectsRecent(LoginUser user){
        return getAllProjectsAdmin(user, projectQueryDAO::getActiveRecentProjects);

    }

    public List<ProjectQueryDTO> getAllProjectsDefault(LoginUser user){
       return getAllProjectsAdmin(user, projectQueryDAO::getDisplayedDefaultProjects);
    }

    public List<ProjectQueryDTO> getApprovedProjectsDefault(LoginUser user){
        return getAllProjectsAdmin(user, projectQueryDAO::get);
    }

    private List<ProjectQueryDTO> getAllProjectsAdmin(LoginUser user, ProjectQueryFunc func){
        return transactionManager.execute(conn ->{
            if(userService.checkNotAdmin(conn, user)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return List.of();
            }
            return func.apply(conn);
        });
    }

    private interface ProjectQueryFunc {

        List<ProjectQueryDTO> apply(Connection connection) throws SQLException;
    }

}
