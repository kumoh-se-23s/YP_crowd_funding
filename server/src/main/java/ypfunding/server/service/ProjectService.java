package ypfunding.server.service;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ProjectsDAO;

import java.util.List;

public class ProjectService extends Service<ProjectsDAO>{
    public ProjectService(TransactionManager transactionManager, ProjectsDAO dao) {
        super(transactionManager, dao);
    }

    public List<ProjectDTO> getAllProjects() {
        return transactionManager.execute(dao::getAllProjects);
    }

}
