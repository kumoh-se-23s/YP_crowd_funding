package ypfunding.server.service;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RejectDTO;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.notifications.PermissionDeniedNotification;
import ypfunding.common.notifications.ProjectNotFoundNotification;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.RejectDAO;
import ypfunding.server.persistence.dao.UsersDAO;
import ypfunding.server.util.NotificationUtils;

import java.util.Collections;
import java.util.List;

public class AdminService extends UserService {

    private final ProjectsDAO projectsDAO;
    private final RejectDAO rejectDAO;


    public AdminService(TransactionManager transactionManager, UsersDAO usersDAO, ProjectsDAO projectsDAO, RejectDAO rejectDAO) {
        super(transactionManager, usersDAO);
        this.projectsDAO = projectsDAO;
        this.rejectDAO = rejectDAO;
    }


    public List<UserDTO> getAllAdmins(LoginUser user){

        return transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, user.userId())){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return Collections.emptyList();
            }
            return usersDAO.getAllAdmins(conn);
        });
    }


    public List<ProjectDTO> getPendingProjects(LoginUser user) {

        return transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, user.userId())){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return Collections.emptyList();
            }
            return projectsDAO.getPendingProjects(conn, user.userId());
        });
    }



    public void approveFund(LoginUser user, long projectId){

        transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, user.userId())){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return null;
            }

            if(projectsDAO.approveProjectUpdate(conn, projectId) != 1){
                NotificationUtils.sendNotification(new ProjectNotFoundNotification(projectId));
                return null;
            }

            return null;
        });
    }

    public void rejectFund(LoginUser user, long projectId, String reason){

        transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, user.userId())){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return null;
            }
            if(projectsDAO.rejectProjectUpdate(conn, projectId) != 1){
                NotificationUtils.sendNotification(new ProjectNotFoundNotification(projectId));
                return null;
            }

            RejectDTO rejectDTO = new RejectDTO();
            rejectDTO.setProjectId(projectId);
            rejectDTO.setCreatedAt(DateTimeUtil.now());
            rejectDTO.setReason(reason);
            rejectDAO.insert(conn, rejectDTO);
            return null;
        });
    }

}
