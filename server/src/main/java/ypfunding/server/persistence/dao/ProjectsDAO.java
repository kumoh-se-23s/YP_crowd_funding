package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLDelete;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLUpdate;
import ypfunding.server.persistence.dao.dynamicsql.col.*;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectsDAO extends DAO {


    public enum Columns implements ColumnsEnum {
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

        Columns(String name) {
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
        SQLInsert sqlInsert = new SQLInsert(Tables.PROJECTS)
                .addTarget(Columns.USER_ID, projectDTO.getUserId())
                .addTarget(Columns.TITLE, projectDTO.getTitle())
                .addTarget(Columns.DESCRIPTION, projectDTO.getDescription())
                .addTarget(Columns.GOAL, projectDTO.getGoal())
                .addTarget(Columns.DURATION, projectDTO.getDuration())
                .addTarget(Columns.APPROVAL_STATUS, projectDTO.getApprovalStatus() != null
                        ? projectDTO.getApprovalStatus().name()
                        : ApprovalStatus.PENDING.name());

        if (projectDTO.getStartDate() != null) {
            sqlInsert.addTarget(Columns.START_DATE, projectDTO.getStartDate());
        }

        sqlInsert.execute(conn);

        //DB내에서는 projectId를 AI로 갱신하지만 자바 메모리엔 갱신이 안되어 직접 갱신
        try (PreparedStatement ps = conn.prepareStatement("SELECT LAST_INSERT_ID()");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                projectDTO.setProjectId(rs.getLong(1));
            }
        }
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    /**
     * 프로젝트 ID로  상세 정보를 조회한다.
     *
     * @param conn      커넥션
     * @param projectId 조회할 프로젝트 ID
     * @return 프로젝트 DTO (존재하지 않으면 null)
     * @throws SQLException DB 오류 발생 시
     */
    public ProjectDTO findById(Connection conn, long projectId) throws SQLException {
        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.PROJECTS))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)))
                .execute(conn)) {
            if (rs.next()) {
                return toDTO(rs);
            }
            return null;
        }
    }

    /**
     * UserId 기반 프로젝트 조회 기능
     * @param conn   커넥션
     * @param userId 메이커 사용자 ID
     * @return 프로젝트 DTO 목록
     * @throws SQLException DB 오류 발생 시
     */
    public List<ProjectDTO> findByUserId(Connection conn, long userId) throws SQLException {
        List<ProjectDTO> result = new ArrayList<>();
        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.PROJECTS))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.USER_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(userId)))
                .orderBy(new SimpleOrderBySelector(Columns.PROJECT_ID, false))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    //전체 조회 기능
    public List<ProjectDTO> getAllProjects(Connection conn) throws SQLException {

        List<ProjectDTO> result = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.PROJECTS))
                .execute(conn)) {

            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    public List<ProjectDTO> getPendingProjects(Connection conn, long userID) throws SQLException {
        List<ProjectDTO> result = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.PROJECTS))
                .addCondition(new BasicCondition(new ColumnValueToken(Columns.USER_ID), ArithmeticConditionOperator.EQ, new UserValueToken(userID)))
                .addCondition(new BasicCondition(new ColumnValueToken(Columns.APPROVAL_STATUS), ArithmeticConditionOperator.EQ, new UserValueToken(ApprovalStatus.PENDING)))
                .execute(conn)) {

            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    public List<ProjectDTO> getDisplayedProjects(Connection conn) throws SQLException{
        return getDisplayedProjects(conn, new SimpleOrderBySelector(Columns.PROJECT_ID,  true));
    }

    // Displayed projects (a.k.a. Approved Projects)
    // User can view this
    public List<ProjectDTO> getDisplayedProjects(Connection conn, OrderBySelector sort) throws SQLException{
        List<ProjectDTO> result = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.PROJECTS))
                .addCondition(new BasicCondition(new ColumnValueToken(Columns.APPROVAL_STATUS), ArithmeticConditionOperator.EQ, new UserValueToken(ApprovalStatus.APPROVED)))
                .orderBy(sort)
                .execute(conn)) {

            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    //ResultSet 묶기
    private ProjectDTO toDTO(ResultSet rs) throws SQLException {
        ProjectDTO dto = new ProjectDTO();
        dto.setProjectId(rs.getLong(Columns.PROJECT_ID.name));
        dto.setUserId(rs.getLong(Columns.USER_ID.name));
        dto.setTitle(rs.getString(Columns.TITLE.name));
        dto.setDescription(rs.getString(Columns.DESCRIPTION.name));
        dto.setGoal(rs.getLong(Columns.GOAL.name));
        dto.setDuration(rs.getInt(Columns.DURATION.name));
        dto.setApprovalStatus(ApprovalStatus.valueOf(rs.getString(Columns.APPROVAL_STATUS.name)));
        dto.setStartDate(rs.getObject(Columns.START_DATE.name, LocalDateTime.class));
        dto.setEndDate(rs.getObject(Columns.END_DATE.name, LocalDateTime.class));

        return dto;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

    //거절된 프로젝트를 수정하고 다시 PENDING 상태로 변경
    public int resubmitRejectedProjectUpdate(Connection conn, ProjectDTO projectDTO) throws SQLException {
        return new SQLUpdate(new DirectReferenceTable(Tables.PROJECTS), new UpdateSelector[]{
                new UpdateSelector(Columns.TITLE, new UserValueToken(projectDTO.getTitle())),
                new UpdateSelector(Columns.DESCRIPTION, new UserValueToken(projectDTO.getDescription())),
                new UpdateSelector(Columns.GOAL, new UserValueToken(projectDTO.getGoal())),
                new UpdateSelector(Columns.DURATION, new UserValueToken(projectDTO.getDuration())),
                new UpdateSelector(Columns.APPROVAL_STATUS, new UserValueToken(ApprovalStatus.PENDING.name()))
        })
                .addCondition(Condition.and(
                        new BasicCondition(new ColumnValueToken(Columns.PROJECT_ID), ArithmeticConditionOperator.EQ, new UserValueToken(projectDTO.getProjectId())),
                        new BasicCondition(new ColumnValueToken(Columns.USER_ID), ArithmeticConditionOperator.EQ, new UserValueToken(projectDTO.getUserId())),
                        new BasicCondition(new ColumnValueToken(Columns.APPROVAL_STATUS), ArithmeticConditionOperator.EQ, new UserValueToken(ApprovalStatus.REJECTED.name()))
                ))
                .execute(conn);

    }

    public int approveProjectUpdate(Connection conn, long projectID) throws SQLException {
        return new SQLUpdate(new DirectReferenceTable(Tables.PROJECTS), new UpdateSelector[]{
                new UpdateSelector(Columns.START_DATE, new UserValueToken(DateTimeUtil.now())),
                new UpdateSelector(Columns.APPROVAL_STATUS, new UserValueToken(ApprovalStatus.APPROVED.name()))
        })
                .addCondition(Condition.and(
                        new BasicCondition(new ColumnValueToken(Columns.PROJECT_ID), ArithmeticConditionOperator.EQ, new UserValueToken(projectID)),
                        new BasicCondition(new ColumnValueToken(Columns.APPROVAL_STATUS), ArithmeticConditionOperator.EQ, new UserValueToken(ApprovalStatus.PENDING.name()))
                ))
                .execute(conn);
    }

    //거절로 update 하는 메소드
    public int rejectProjectUpdate(Connection conn, long projectId) throws SQLException {
        return new SQLUpdate(new DirectReferenceTable(Tables.PROJECTS), new UpdateSelector[]{
                new UpdateSelector(Columns.APPROVAL_STATUS, new UserValueToken(ApprovalStatus.REJECTED.name()))
        })
                .addCondition(Condition.and(
                        new BasicCondition(new ColumnValueToken(Columns.PROJECT_ID), ArithmeticConditionOperator.EQ, new UserValueToken(projectId)),
                        new BasicCondition(new ColumnValueToken(Columns.APPROVAL_STATUS), ArithmeticConditionOperator.EQ, new UserValueToken(ApprovalStatus.PENDING.name()))
                ))
                .execute(conn);

    }

    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

    public void delete(Connection conn, long projectId) throws SQLException {
        new SQLDelete(Tables.PROJECTS)
                .addCondition(new BasicCondition(new UserValueToken(projectId), ArithmeticConditionOperator.EQ, new ColumnValueToken(Columns.PROJECT_ID)))
                .execute(conn);
    }


}
