package ypfunding.server.service;

import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.notifications.PermissionDeniedNotification;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.ProjectFundingStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.*;
import ypfunding.server.persistence.dao.dynamicsql.col.SimpleOrderBySelector;
import ypfunding.server.util.NotificationUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class ProjectQueryService extends Service {

    private final UserService userService;
    private final ProjectQueryDAO projectQueryDAO;

    public ProjectQueryService(TransactionManager transactionManager, UserService userService) {
        super(transactionManager);
        this.userService = userService;
        this.projectQueryDAO = new ProjectQueryDAO();
    }

    public List<ProjectQueryDTO> getSelectedProjectWithStatus(LoginUser user, long projectId) {
        return transactionManager.execute(conn -> {
            if (userService.checkNotAdmin(conn, user)) {
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return List.of();
            }
            return projectQueryDAO.getProject(conn, projectId);
        });
    }

    public List<ProjectQueryDTO> getAllActiveProjectsRecent(LoginUser user) {
        return getAllActiveProjects(user, new SimpleOrderBySelector(ProjectsDAO.Columns.START_DATE, false));
    }

    public List<ProjectQueryDTO> getAllActiveProjects(LoginUser user, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getActiveProjects(e, sorter), UserType.values());
    }

    public List<ProjectQueryDTO> searchActiveProjects(LoginUser user, String projectName, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getActiveProjectsForName(e, projectName, sorter), UserType.values());
    }


    /**
     * Maker-only function.
     * @param user requested user
     * @param approvalStatus filtering status. can be null.
     * @param fundingStatus filtering status. can be null.
     * @param sorter filtering status. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> getMyProjects(LoginUser user, ApprovalStatus approvalStatus, ProjectFundingStatus fundingStatus, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getMyProjects(e, user.userId(), approvalStatus, fundingStatus, sorter), UserType.MAKER);
    }

    /**
     * Maker-only function.
     * @param user requested user
     * @param projectName filtering status. if empty, return all projects.
     * @param approvalStatus filtering status. can be null.
     * @param fundingStatus filtering status. can be null.
     * @param sorter filtering status. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> getMyProjectsForName(LoginUser user, String projectName, ApprovalStatus approvalStatus, ProjectFundingStatus fundingStatus, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getMyProjectsForName(e, user.userId(), projectName, approvalStatus, fundingStatus, sorter), UserType.MAKER);
    }

    public List<ProjectQueryDTO> getAllPendingProjects(LoginUser user, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getPendingProjects(e, sorter), UserType.ADMIN);
    }

    public List<ProjectQueryDTO> searchPendingProjects(LoginUser user, String projectName, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getPendingProjectsForName(e, projectName, sorter), UserType.ADMIN);
    }


    private List<ProjectQueryDTO> getProjects(LoginUser user, ProjectQueryFunc func, UserType... permission) {

        return transactionManager.execute(conn -> {
            UserDTO dto = userService.usersDAO.getUserById(conn, user.userId());
            if (dto == null || !Set.of(permission).contains(dto.getUserType())) {
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
