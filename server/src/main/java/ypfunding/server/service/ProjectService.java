package ypfunding.server.service;

import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.common.dto.ProjectDTO;

import java.sql.SQLException;
import java.util.List;

public class ProjectService extends Service<ProjectsDAO>{
    public ProjectService(ProjectsDAO dao) {
        super(dao);
    }

    public List<ProjectDTO> getAllProjects() throws SQLException {
        return dao.getAllProjects();
    }

}
