package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ProjectDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectsDAO extends DAO {

    public enum Columns{
        ID("id"),
        TITLE("title"),
        DURATION("duration"),
        START_DATE("start_date"),
        GOAL("goal"),
        WRITER("writer"),
        APPROVAL_STATUS("approval_status");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void fillDataFromResultSet(ProjectDTO projectDTO, PreparedStatement psmt) throws SQLException {
        psmt.setString(1, projectDTO.getTitle());
        psmt.setInt(2, projectDTO.getDuration());
        psmt.setDate(3, new Date(projectDTO.getStartDate().getTime()));
        psmt.setLong(4, projectDTO.getGoal());
        psmt.setLong(5,projectDTO.getWriterID());
        psmt.setString(6,projectDTO.getApprovalStatus().name());
    }
    //Create(생성) 좋아요 추가

    public void insert(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO projects (title, duration, startDate, goal, writerId, approvalStatus) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)) {

            fillDataFromResultSet(projectDTO, psmt);

            psmt.executeUpdate();
        }
    }

    public ProjectDTO getById(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String GET_BY_ID_SQL = "SELECT * FROM projects WHERE id = ?";
        ProjectDTO dto = null;

        try (PreparedStatement psmt = conn.prepareStatement(GET_BY_ID_SQL)) {
            psmt.setLong(1, projectDTO.getId());


            try (ResultSet rs = psmt.executeQuery()) {
                if (rs.next()) {
                    dto = new ProjectDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setTitle(rs.getString("title"));
                    dto.setDuration(rs.getInt("duration"));
                    dto.setStartDate(rs.getDate("startDate"));
                    dto.setGoal(rs.getLong("goal"));
                    dto.setWriterID(rs.getLong("writerId"));
                    dto.setApprovalStatus(ProjectDTO.ApprovalStatus.valueOf(rs.getString("approvalStatus")));
                    return dto;

                }
            }
        }
        return dto;
    }
    public List<ProjectDTO> getAllProjects(Connection conn) throws SQLException {
        final String sql = "select * from projects";
        List<ProjectDTO> result = new ArrayList<>();

        try(Statement statement = conn.createStatement()){
            ResultSet rs = statement.executeQuery(sql);
            while(rs.next()){
                ProjectDTO dto = new ProjectDTO();

                dto.setId(rs.getLong(Columns.ID.name));
                dto.setTitle(rs.getString(Columns.TITLE.name));
                dto.setDuration(rs.getInt(Columns.DURATION.name));
                dto.setStartDate(rs.getDate(Columns.START_DATE.name));
                dto.setGoal(rs.getLong(Columns.GOAL.name));
                dto.setWriterID(rs.getLong(Columns.WRITER.name));
                dto.setApprovalStatus(ProjectDTO.ApprovalStatus.valueOf(rs.getString(Columns.APPROVAL_STATUS.name)));

                result.add(dto);
            }
        }
        return result;
    }


    //Update(수정) 기능
    public void update(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String UPDATE_SQL = "UPDATE projects SET title = ?, duration = ?, startDate = ?, goal = ?, writerId = ?, approvalStatus = ?  WHERE id = ?";

        try (PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)) {
            fillDataFromResultSet(projectDTO, psmt);
            psmt.setLong(7,projectDTO.getId());

            psmt.executeUpdate();
        }
    }

    public void delete(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String DELETE_SQL = "DELETE FROM projects WHERE id = ?";
        try (PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)) {
            psmt.setLong(1, projectDTO.getId());
            psmt.executeUpdate();
        }
    }
}
