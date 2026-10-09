package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.ProjectsDAO;
import kr.yp_crowdfunding.persistence.dto.ProjectDTO;

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
