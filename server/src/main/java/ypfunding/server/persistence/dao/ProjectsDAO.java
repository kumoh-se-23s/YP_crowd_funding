package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.server.util.DynamicSQLUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectsDAO extends DAO {

    public enum Columns implements ColumnsEnum{
        PROJECT_ID("project_id"),
        USER_ID("user_id"),
        TITLE("title"),
        DESCRIPTION("description"),
        GOAL("goal"),
        DURATION("duration"),
        START_DATE("start_date"),
        END_DATE("end_date"),
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
        psmt.setLong(5,projectDTO.getUserId());
        psmt.setString(6,projectDTO.getApprovalStatus().name());
    }
    //Create(생성) 좋아요 추가

    //endDate는 유도속성으로 insert에서는 제외
    public void insert(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String INSERT_SQL = DynamicSQLUtil.generate("INSERT INTO projects ({}) VALUES ({})",
                Columns.USER_ID, Columns.TITLE, Columns.DESCRIPTION, Columns.GOAL,
                Columns.START_DATE, Columns.DURATION, Columns.APPROVAL_STATUS);

        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setLong(1, projectDTO.getUserId());
            ps.setString(2, projectDTO.getTitle());
            ps.setString(3, projectDTO.getDescription());
            ps.setLong(4, projectDTO.getGoal());
            ps.setInt(5, projectDTO.getDuration());
            //기본 insert때 pending으로 지정
            ps.setString(6, ProjectDTO.ApprovalStatus.PENDING.name());

            ps.executeUpdate();
        }
    }
    public ProjectDTO findById(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String FIND_BY_ID_SQL = DynamicSQLUtil.generate("SELECT * FROM projects WHERE project_id = ({})",
                Columns.PROJECT_ID);
        ProjectDTO dto = null;

        try (PreparedStatement psmt = conn.prepareStatement(FIND_BY_ID_SQL)) {
            psmt.setLong(1, projectDTO.getProjectId());


            try (ResultSet rs = psmt.executeQuery()) {
                if (rs.next()) {
                    dto = new ProjectDTO();
                    dto.setProjectId(rs.getLong("id"));
                    dto.setTitle(rs.getString("title"));
                    dto.setDuration(rs.getInt("duration"));
                    dto.setStartDate(rs.getDate("startDate"));
                    dto.setGoal(rs.getLong("goal"));
                    dto.setUserId(rs.getLong("writerId"));
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

                dto.setProjectId(rs.getLong(Columns.PROJECT_ID.name));
                dto.setUserId(rs.getLong(Columns.USER_ID.name));
                dto.setTitle(rs.getString(Columns.TITLE.name));
                dto.setDuration(rs.getInt(Columns.DURATION.name));
                dto.setStartDate(rs.getDate(Columns.START_DATE.name));
                dto.setGoal(rs.getLong(Columns.GOAL.name));
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
            psmt.setLong(7,projectDTO.getProjectId());

            psmt.executeUpdate();
        }
    }

    public void delete(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String DELETE_SQL = "DELETE FROM projects WHERE id = ?";
        try (PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)) {
            psmt.setLong(1, projectDTO.getProjectId());
            psmt.executeUpdate();
        }
    }
}
