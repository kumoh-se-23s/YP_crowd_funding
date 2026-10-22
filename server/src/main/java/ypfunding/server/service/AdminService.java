package ypfunding.server.service;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RejectDTO;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.RejectDAO;
import ypfunding.server.persistence.dao.UsersDAO;
import ypfunding.server.util.NotificationUtils;
import ypfunding.server.util.TimeUtils;

import java.sql.Connection;
import java.sql.SQLException;

public class AdminService extends Service {

    private final UsersDAO usersDAO;
    private final ProjectsDAO projectsDAO;
    private final RejectDAO rejectDAO;

    public AdminService(TransactionManager transactionManager, UsersDAO usersDAO, ProjectsDAO projectsDAO, RejectDAO rejectDAO) {
        super(transactionManager);
        this.usersDAO = usersDAO;
        this.projectsDAO = projectsDAO;
        this.rejectDAO = rejectDAO;
    }

    public boolean checkNotAdmin(Connection conn, long userID) throws SQLException {
        UserDTO dto = usersDAO.getUserById(conn, userID);
        return dto == null || dto.getUserType() != UserType.ADMIN;
    }

    public void approveFund(long userID, long projectID){

        transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, userID)){
                NotificationUtils.sendNotification("User is not admin");
                return null;
            }

            if(projectsDAO.approveProjectUpdate(conn, projectID) != 1){
                NotificationUtils.sendNotification("Approve target project is invalid or doesn't exist");
                return null;
            }

            return null;
        });
    }

    public void rejectFund(long userID, long projectID, String reason){
        transactionManager.execute(conn -> {
            if(checkNotAdmin(conn, userID)){
                NotificationUtils.sendNotification("User is not admin");
                return null;
            }

            if(projectsDAO.rejectProjectUpdate(conn, projectID) != 1){
                NotificationUtils.sendNotification("Reject target project is invalid or doesn't exist");
                return null;
            }

            RejectDTO rejectDTO = new RejectDTO();
            rejectDTO.setProjectID(projectID);
            rejectDTO.setCreatedAt(TimeUtils.getCurrentTime());
            rejectDTO.setReason(reason);
            rejectDAO.insert(conn, rejectDTO);
            return null;
        });
    }

}
