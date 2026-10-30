package ypfunding.server.service;

import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.notifications.PermissionDeniedNotification;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.Category;
import ypfunding.common.type.ProjectFundingStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.*;
import ypfunding.server.persistence.dao.dynamicsql.col.SimpleOrderBySelector;
import ypfunding.server.util.NotificationUtils;
import ypfunding.server.util.UserUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class ProjectQueryService extends Service {

    private final ProjectQueryDAO projectQueryDAO;

    public ProjectQueryService(TransactionManager transactionManager) {
        super(transactionManager);
        this.projectQueryDAO = new ProjectQueryDAO();
    }

    public List<ProjectQueryDTO> getSelectedProjectWithStatus(LoginUser user, long projectId) {
        return transactionManager.execute(conn -> {
            if (UserUtils.checkNotAdmin(user)) {
                NotificationUtils.sendNotification(new PermissionDeniedNotification());
                return List.of();
            }
            return projectQueryDAO.getProject(conn, projectId);
        });
    }
    /**
     * Admin-only function.
     * @param user requested user
     * @param sorter filters status. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> getAllProjects(LoginUser user, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getAllProjects(e, sorter), UserType.ADMIN);
    }

    /**
     * Everyone can call this function.
     * @param user requested user
     * @return result projects sorted by most recent
     */
    public List<ProjectQueryDTO> getAllActiveProjectsRecent(LoginUser user) {
        return getAllActiveProjects(user, new SimpleOrderBySelector(ProjectsDAO.Columns.START_DATE, false));
    }
    /**
     * Everyone can call this function.
     * @param user requested user
     * @param sorter filters status. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> getAllActiveProjects(LoginUser user, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getActiveProjects(e, sorter), UserType.values());
    }

    /**
     * Everyone can call this function.
     * @param user requested user
     * @param category filters category. cannot be null.
     * @param sorter filters status. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> filterEndedProjects(LoginUser user, Category category, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getFilteredEndedProjects(e, category, sorter), UserType.values());
    }

    /**
     * Everyone can call this function.
     * @param user requested user
     * @param category filters category. cannot be null.
     * @param sorter filters status. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> filterActiveProjects(LoginUser user, Category category, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getFilteredActiveProjects(e, category, sorter), UserType.values());
    }

    /**
     * Everyone can call this function.
     * @param user requested user
     * @param projectName filters project name. if empty, return all projects.
     * @param sorter filters status. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> searchActiveProjects(LoginUser user, String projectName, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getActiveProjectsForName(e, projectName, sorter), UserType.values());
    }


    /**
     * Maker-only function.
     * @param user requested user
     * @param approvalStatus filters status. can be null.
     * @param fundingStatus filters status. can be null.
     * @param sorter sorting order. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> getMyProjects(LoginUser user, ApprovalStatus approvalStatus, ProjectFundingStatus fundingStatus, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getMyProjects(e, user.userId(), approvalStatus, fundingStatus, sorter), UserType.MAKER);
    }

    /**
     * Maker-only function.
     * @param user requested user
     * @param projectName filters project name. if empty, return all projects.
     * @param approvalStatus filters status. can be null.
     * @param fundingStatus filters status. can be null.
     * @param sorter sorting order. if empty, use default sorting (ID).
     * @return result projects
     */
    public List<ProjectQueryDTO> getMyProjectsForName(LoginUser user, String projectName, ApprovalStatus approvalStatus, ProjectFundingStatus fundingStatus, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getMyProjectsForName(e, user.userId(), projectName, approvalStatus, fundingStatus, sorter), UserType.MAKER);
    }

    /**
     * Admin-only function. Get all pending projects
     * @param user requested user
     * @param sorter sorting order. if empty, use default sorting (ID).
     * @return result projects sorted by most recent
     */
    public List<ProjectQueryDTO> getAllPendingProjects(LoginUser user, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getPendingProjects(e, sorter), UserType.ADMIN);
    }
    /**
     * Admin-only function. Get all pending projects
     * @param user requested user
     * @param projectName filters project name. if empty, return all projects.
     * @param sorter sorting order. if empty, use default sorting (ID).
     * @return result projects sorted by most recent
     */
    public List<ProjectQueryDTO> searchPendingProjects(LoginUser user, String projectName, SimpleOrderBySelector... sorter) {
        return getProjects(user, e -> projectQueryDAO.getPendingProjectsForName(e, projectName, sorter), UserType.ADMIN);
    }


    private List<ProjectQueryDTO> getProjects(LoginUser user, ProjectQueryFunc func, UserType... permission) {

        return transactionManager.execute(conn -> {

            if (!Set.of(permission).contains(user.userType())) {
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
