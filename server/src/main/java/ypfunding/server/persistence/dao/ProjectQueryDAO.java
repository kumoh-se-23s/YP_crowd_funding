package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.col.*;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinType;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinedTable;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ProjectQueryDAO extends DAO {

    private enum Columns implements ColumnsEnum {
        PROJECT_SOLD_PRICE("project_sold_price"),
        PROJECT_FUND_PROGRESS("project_fund_progress"),
        SUPPORTER_COUNT("supporter_count"),
        REWARD_SOLD_STOCK("reward_sold_stock"),
        REWARD_SOLD_PRICE("reward_sold_price"),
        REWARD_REMAINING_STOCK("remaining_stock");

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


    // =================================================================
    // 2. READ SECTION
    // =================================================================

    private SQLSelect createRewardStatusSQLBuilder() {
        DirectReferenceTable cteFunds = new DirectReferenceTable(Tables.FUNDS);
        DirectReferenceTable cteRewards = new DirectReferenceTable(Tables.REWARDS);
        return new SQLSelect(
                new ColumnSelector[]{
                        new ColumnFromTokenSelector(
                                RewardsDAO.Columns.REWARD_ID,
                                new TableRefColumnToken(cteRewards, RewardsDAO.Columns.REWARD_ID)),
                        new ColumnFromTokenSelector(
                                Columns.REWARD_REMAINING_STOCK,
                                new ValueExpressionToken(
                                        new TableRefColumnToken(cteRewards, RewardsDAO.Columns.STOCK),
                                        ArithmeticOperator.SUB,
                                        new BuiltinFunctionToken(
                                                BuiltinFunctions.SUM,
                                                new TableRefColumnToken(cteFunds, FundsDAO.Columns.QUANTITY)
                                        )
                                )
                        ),
                        new ColumnFromTokenSelector(
                                Columns.REWARD_SOLD_STOCK,
                                new BuiltinFunctionToken(
                                        BuiltinFunctions.SUM,
                                        new TableRefColumnToken(cteFunds, FundsDAO.Columns.QUANTITY)
                                )
                        ),
                        new ColumnFromTokenSelector(
                                Columns.REWARD_SOLD_PRICE,
                                new ValueExpressionToken(
                                        new BuiltinFunctionToken(
                                                BuiltinFunctions.SUM,
                                                new TableRefColumnToken(cteFunds, FundsDAO.Columns.QUANTITY)
                                        ),
                                        ArithmeticOperator.MUL,
                                        new TableRefColumnToken(cteRewards, RewardsDAO.Columns.PRICE)
                                )
                        )
                },
                new JoinedTable(
                        cteRewards,
                        JoinType.LEFT,
                        cteFunds,
                        new BasicCondition(
                                new TableRefColumnToken(cteRewards, RewardsDAO.Columns.REWARD_ID),
                                ArithmeticConditionOperator.EQ,
                                new TableRefColumnToken(cteFunds, FundsDAO.Columns.REWARD_ID)
                        )
                )
        ).groupBy(FundsDAO.Columns.REWARD_ID);
    }


    private SQLSelect createProjectStatusSQLBuilder() {

        DirectReferenceTable projectsTable = new DirectReferenceTable(Tables.PROJECTS);
        DirectReferenceTable rewardsTable = new DirectReferenceTable(Tables.REWARDS);
        DirectReferenceTable cteProjects = new DirectReferenceTable(Tables.PROJECTS);

        PredefinedReferenceTable cteRewardStatus = new PredefinedReferenceTable(createRewardStatusSQLBuilder());
        PredefinedReferenceTable cteProjectsWithStatus = new PredefinedReferenceTable(
                new SQLSelect(
                        new ColumnSelector[]{
                                new ColumnFromTokenSelector(
                                        ProjectsDAO.Columns.PROJECT_ID,
                                        new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID)),
                                new ColumnFromTokenSelector(
                                        Columns.PROJECT_SOLD_PRICE,
                                        new BuiltinFunctionToken(BuiltinFunctions.SUM,
                                                new BuiltinFunctionToken(
                                                        BuiltinFunctions.IFNULL,
                                                        new TableRefColumnToken(cteRewardStatus, Columns.REWARD_SOLD_PRICE),
                                                        new UserValueToken(0)
                                                )
                                        )
                                ),
                                new ColumnFromTokenSelector(
                                        Columns.SUPPORTER_COUNT,
                                        new BuiltinFunctionToken(
                                                BuiltinFunctions.COUNT,
                                                new WildcardValueToken()
                                        )
                                ),
                                new ColumnFromTokenSelector(
                                        ProjectsDAO.Columns.GOAL,
                                        new TableRefColumnToken(cteProjects, ProjectsDAO.Columns.GOAL)),
                                new ColumnFromTokenSelector(
                                        Columns.PROJECT_FUND_PROGRESS,
                                        new ValueExpressionToken(
                                                new BuiltinFunctionToken(BuiltinFunctions.SUM,
                                                        new BuiltinFunctionToken(
                                                                BuiltinFunctions.IFNULL,
                                                                new TableRefColumnToken(cteRewardStatus, Columns.REWARD_SOLD_PRICE),
                                                                new UserValueToken(0)
                                                        )
                                                ),
                                                ArithmeticOperator.DIV,
                                                new TableRefColumnToken(cteProjects, ProjectsDAO.Columns.GOAL)
                                        )
                                )
                        },
                        new JoinedTable(
                                cteProjects,
                                JoinType.LEFT,
                                cteRewardStatus,
                                new BasicCondition(
                                        new TableRefColumnToken(cteProjects, ProjectsDAO.Columns.PROJECT_ID),
                                        ArithmeticConditionOperator.EQ,
                                        new TableRefColumnToken(cteRewardStatus, RewardsDAO.Columns.PROJECT_ID)
                                )
                        )
                ).groupBy(new TableRefColumnToken(cteProjects, ProjectsDAO.Columns.PROJECT_ID))
        );
        return new SQLSelect(new ColumnSelector[]{
                new ColumnFromTokenSelector(ProjectsDAO.Columns.PROJECT_ID, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.USER_ID, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.USER_ID)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.TITLE, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.TITLE)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.DESCRIPTION, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.DESCRIPTION)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.GOAL, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.GOAL)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.DURATION, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.DURATION)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.START_DATE, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.START_DATE)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.END_DATE, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.END_DATE)),
                new ColumnFromTokenSelector(ProjectsDAO.Columns.APPROVAL_STATUS, new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.APPROVAL_STATUS)),
                new ColumnFromTokenSelector(Columns.SUPPORTER_COUNT, new TableRefColumnToken(cteProjectsWithStatus, Columns.SUPPORTER_COUNT)),
                new ColumnFromTokenSelector(Columns.PROJECT_SOLD_PRICE, new TableRefColumnToken(cteProjectsWithStatus, Columns.PROJECT_SOLD_PRICE)),
                new ColumnFromTokenSelector(Columns.PROJECT_FUND_PROGRESS, new TableRefColumnToken(cteProjectsWithStatus, Columns.PROJECT_FUND_PROGRESS)),
        }, new JoinedTable(
                projectsTable,
                JoinType.LEFT,
                rewardsTable,
                new BasicCondition(
                        new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.PROJECT_ID)
                )
        ), cteRewardStatus, cteProjectsWithStatus);
    }

    public List<ProjectQueryDTO> getProject(Connection conn, long projectId) throws SQLException {

        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(new BasicCondition(new ColumnValueToken(RewardsDAO.Columns.PROJECT_ID), ArithmeticConditionOperator.EQ, new UserValueToken(projectId)))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    public final List<ProjectQueryDTO> getActiveRecentProjects(Connection conn) throws SQLException {
        return queryActiveProjects(conn, () ->
                new OrderBySelector(new ColumnValueToken(ProjectsDAO.Columns.START_DATE), false));
    }

    public final List<ProjectQueryDTO> getActiveProjects(Connection conn) throws SQLException {
        return getActiveProjects(conn);
    }




    @SafeVarargs
    private List<ProjectQueryDTO> queryActiveProjects(Connection conn, Supplier<OrderBySelector>... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(getProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(getEndedCondition(false))
                .orderBy(Arrays.stream(sorter).map(Supplier::get).toArray(OrderBySelector[]::new))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    @SafeVarargs
    private List<ProjectQueryDTO> queryFailedProjects(Connection conn, Supplier<OrderBySelector>... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(getProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(getEndedCondition(true))
                .addCondition(getProjectProgressCondition(false))
                .orderBy(Arrays.stream(sorter).map(Supplier::get).toArray(OrderBySelector[]::new))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    @SafeVarargs
    private List<ProjectQueryDTO> querySuccessProjects(Connection conn, Supplier<OrderBySelector>... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(getProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(getEndedCondition(true))
                .addCondition(getProjectProgressCondition(true))
                .orderBy(Arrays.stream(sorter).map(Supplier::get).toArray(OrderBySelector[]::new))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    private BasicCondition getProjectApprovalStatusCondition(ApprovalStatus approvalStatus){
        return new BasicCondition(
                new ColumnValueToken(ProjectsDAO.Columns.APPROVAL_STATUS),
                ArithmeticConditionOperator.EQ,
                new UserValueToken(approvalStatus)
        );
    }

    private BasicCondition getEndedCondition(boolean isEnd){
        return new BasicCondition(
                new ColumnValueToken(ProjectsDAO.Columns.END_DATE),
                isEnd ? ArithmeticConditionOperator.LE : ArithmeticConditionOperator.GT,
                new UserValueToken(
                        DateTimeUtil.now()
                )
        );
    }

    private BasicCondition getProjectProgressCondition(boolean isSuccess){
        return new BasicCondition(
                new ColumnValueToken(Columns.PROJECT_FUND_PROGRESS),
                isSuccess ? ArithmeticConditionOperator.GE : ArithmeticConditionOperator.LT,
                new UserValueToken(1)
        );
    }

    @SafeVarargs
    private List<ProjectQueryDTO> queryEndedProjects(Connection conn, Supplier<OrderBySelector>... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(getProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(getEndedCondition(true))
                .orderBy(Arrays.stream(sorter).map(Supplier::get).toArray(OrderBySelector[]::new))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    @SafeVarargs
    private List<ProjectQueryDTO> queryPendingProjects(Connection conn, Supplier<OrderBySelector>... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(getProjectApprovalStatusCondition(ApprovalStatus.PENDING))
                .orderBy(Arrays.stream(sorter).map(Supplier::get).toArray(OrderBySelector[]::new))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    @SafeVarargs
    private List<ProjectQueryDTO> queryRejectedProjects(Connection conn, Supplier<OrderBySelector>... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(getProjectApprovalStatusCondition(ApprovalStatus.REJECTED))
                .orderBy(Arrays.stream(sorter).map(Supplier::get).toArray(OrderBySelector[]::new))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    // Displayed projects with funding status
    // Admin only.
    @SafeVarargs
    private List<ProjectQueryDTO> queryAllProjects(Connection conn, Supplier<OrderBySelector>... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .orderBy(Arrays.stream(sorter).map(Supplier::get).toArray(OrderBySelector[]::new))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    private ProjectQueryDTO toDTO(ResultSet rs) throws SQLException {
        ProjectQueryDTO dto = new ProjectQueryDTO();
        dto.setProjectId(rs.getLong(ProjectsDAO.Columns.PROJECT_ID.toString()));
        dto.setUserId(rs.getLong(ProjectsDAO.Columns.USER_ID.toString()));
        dto.setTitle(rs.getString(ProjectsDAO.Columns.TITLE.toString()));
        dto.setDescription(rs.getString(ProjectsDAO.Columns.DESCRIPTION.toString()));
        dto.setGoal(rs.getLong(ProjectsDAO.Columns.GOAL.toString()));
        dto.setDuration(rs.getInt(ProjectsDAO.Columns.DURATION.toString()));
        dto.setApprovalStatus(ApprovalStatus.valueOf(rs.getString(ProjectsDAO.Columns.APPROVAL_STATUS.toString())));
        dto.setStartDate(rs.getObject(ProjectsDAO.Columns.START_DATE.toString(), LocalDateTime.class));
        dto.setEndDate(rs.getObject(ProjectsDAO.Columns.END_DATE.toString(), LocalDateTime.class));
        dto.setSupporterCount(rs.getLong(Columns.SUPPORTER_COUNT.toString()));
        dto.setSoldPrice(rs.getLong(Columns.PROJECT_SOLD_PRICE.toString()));
        dto.setFundProgress(rs.getLong(Columns.PROJECT_FUND_PROGRESS.toString()));
        return dto;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================


    // =================================================================
    // 4. DELETE SECTION
    // =================================================================


}
