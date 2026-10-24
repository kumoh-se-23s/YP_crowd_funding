package ypfunding.server.service;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RejectDTO;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.notifications.PermissionDeniedNotification;
import ypfunding.common.notifications.ProjectNotFoundNotification;
import ypfunding.common.util.DateTimeUtil;
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


    public List<UserDTO> getAllAdmins(long userID){

        return transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, userID)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return Collections.emptyList();
            }
            return usersDAO.getAllAdmins(conn);
        });
    }


    public List<ProjectDTO> getPendingProjects(long userID) {

        return transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, userID)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return Collections.emptyList();
            }
            return projectsDAO.getPendingProjects(conn, userID);
        });
    }



    public void approveFund(long userID, long projectID){

        transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, userID)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return null;
            }

            if(projectsDAO.approveProjectUpdate(conn, projectID) != 1){
                NotificationUtils.sendNotification(new ProjectNotFoundNotification(projectID));
                return null;
            }

            return null;
        });
    }

    public void rejectFund(long userID, long projectID, String reason){

        transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, userID)){
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return null;
            }
            if(projectsDAO.rejectProjectUpdate(conn, projectID) != 1){
                NotificationUtils.sendNotification(new ProjectNotFoundNotification(projectID));
                return null;
            }

            RejectDTO rejectDTO = new RejectDTO();
            rejectDTO.setProjectID(projectID);
            rejectDTO.setCreatedAt(DateTimeUtil.now());
            rejectDTO.setReason(reason);
            rejectDAO.insert(conn, rejectDTO);
            return null;
        });
    }

}
