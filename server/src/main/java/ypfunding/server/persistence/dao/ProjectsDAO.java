package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.server.util.DynamicSQLUtil;

import java.sql.*;
import java.time.LocalDateTime;
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

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    //endDate는 유도속성으로 insert에서는 제외
    public void insert(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String INSERT_SQL = DynamicSQLUtil.generate("INSERT INTO projects ({}) VALUES ({})",
                Columns.USER_ID, Columns.TITLE, Columns.DESCRIPTION, Columns.GOAL,
                Columns.START_DATE, Columns.DURATION, Columns.APPROVAL_STATUS);

        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            setInsertParameters(ps, projectDTO);
            ps.executeUpdate();
        }
    }
    //insert 내부 파라미터 설정
    private void setInsertParameters(PreparedStatement ps, ProjectDTO projectDTO) throws SQLException {
        ps.setLong(1, projectDTO.getUserId());
        ps.setString(2, projectDTO.getTitle());
        ps.setString(3, projectDTO.getDescription());
        ps.setLong(4, projectDTO.getGoal());
        ps.setObject(5, projectDTO.getStartDate());
        ps.setInt(6, projectDTO.getDuration());
        //기본 insert때 pending으로 지정
        ps.setString(7, ProjectDTO.ApprovalStatus.PENDING.name());
    }

    private void fillDataFromResultSet(ProjectDTO projectDTO, PreparedStatement psmt) throws SQLException {
        psmt.setString(1, projectDTO.getTitle());
        psmt.setInt(2, projectDTO.getDuration());
        psmt.setObject(3, projectDTO.getStartDate());
        psmt.setLong(4, projectDTO.getGoal());
        psmt.setLong(5,projectDTO.getUserId());
        psmt.setString(6,projectDTO.getApprovalStatus().name());
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    //프로젝트ID를 통해 상세 조회
    //무거운 dto말고 projectId를 넘겨 받아서 optimize
    public ProjectDTO findById(Connection conn, long projectId) throws SQLException {
        final String FIND_BY_ID_SQL = "SELECT * FROM projects WHERE project_id = ?";
        ProjectDTO dto = null;

        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            ps.setLong(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toDTO(rs);
                }
                return null;
            }
        }
    }

    //전체 조회 기능
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
                dto.setStartDate(rs.getObject("start_date", LocalDateTime.class));
                dto.setGoal(rs.getLong(Columns.GOAL.name));
                dto.setApprovalStatus(ProjectDTO.ApprovalStatus.valueOf(rs.getString(Columns.APPROVAL_STATUS.name)));

                result.add(dto);
                //이거로 해도 될듯요
                //result.add(toDTO(rs));
            }
        }
        return result;
    }

    private ProjectDTO toDTO(ResultSet rs) throws SQLException {
        ProjectDTO dto = new ProjectDTO();
        dto.setProjectId(rs.getLong("project_id"));
        dto.setUserId(rs.getLong("user_id"));
        dto.setTitle(rs.getString("title"));
        dto.setDescription(rs.getString("description"));
        dto.setGoal(rs.getLong("goal"));
        dto.setDuration(rs.getInt("duration"));
        dto.setApprovalStatus(ProjectDTO.ApprovalStatus.valueOf(rs.getString("approvalStatus")));

        dto.setStartDate(rs.getObject("start_date", LocalDateTime.class));
        dto.setEndDate(rs.getObject("end_date", LocalDateTime.class));

        return dto;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

    //거절된 프로젝트를 수정하고 다시 PENDING 상태로 변경
    public int updateRejected(Connection conn, ProjectDTO projectDTO) throws SQLException {
        String sql = """
                UPDATE projects 
                   SET title = ?, description = ?, goal = ?, duration = ?, approval_status = ?
                 WHERE project_id = ? AND user_id = ? AND approval_status = 'REJECTED'
                """;

        try (PreparedStatement psmt = conn.prepareStatement(sql)) {
            setUpdateParameters(psmt, projectDTO);

            return psmt.executeUpdate();
        }
    }
    //거절로 update 하는 메소드
    public int updateStatusToRejected(Connection conn, long projectId) throws SQLException {
        String sql = """
                UPDATE projects 
                   SET approval_status = 'REJECTED'
                 WHERE project_id = ? AND approval_status = 'PENDING'
                """;

        try (PreparedStatement psmt = conn.prepareStatement(sql)) {
            psmt.setLong(1, projectId);

            return psmt.executeUpdate();
        }
    }

    private void setUpdateParameters(PreparedStatement psmt, ProjectDTO projectDTO) throws SQLException {
        psmt.setString(1, projectDTO.getTitle());
        psmt.setString(2, projectDTO.getDescription());
        psmt.setLong(3, projectDTO.getGoal());
        psmt.setInt(4, projectDTO.getDuration());

        // PENDING으로 업데이트
        psmt.setString(5, ProjectDTO.ApprovalStatus.PENDING.name());

        // WHERE 조건 바인딩
        psmt.setLong(6, projectDTO.getProjectId());
        psmt.setLong(7, projectDTO.getUserId()); // 본인 소유 검증
    }

    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

    public void delete(Connection conn, ProjectDTO projectDTO) throws SQLException {
        final String DELETE_SQL = "DELETE FROM projects WHERE id = ?";
        try (PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)) {
            psmt.setLong(1, projectDTO.getProjectId());
            psmt.executeUpdate();
        }
    }
}
