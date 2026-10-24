package ypfunding.server.service;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ProjectsDAO;

import java.util.List;

public class ProjectService extends Service{
    private final ProjectsDAO projectsDAO;
    public ProjectService(TransactionManager transactionManager, ProjectsDAO projectsDAO) {
        super(transactionManager);
        this.projectsDAO = projectsDAO;
    }

    public List<ProjectDTO> getAllProjects() {
        return transactionManager.execute(projectsDAO::getAllProjects);
    }

}
