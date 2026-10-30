package ypfunding.server.persistence.dao;

import ypfunding.common.dto.FundDTO;
import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.Category;
import ypfunding.common.type.ProjectFundingStatus;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.col.*;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticOperator;
import ypfunding.server.persistence.dao.dynamicsql.op.SetConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.*;
import ypfunding.server.persistence.dao.dynamicsql.token.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectQueryDAO extends DAO {

    private enum Columns implements ColumnsEnum {
        PROJECT_SOLD_PRICE("project_sold_price"),
        PROJECT_FUND_PROGRESS("project_fund_progress"),
        PROJECT_FUND_STATUS("project_fund_status"),
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

    private static class ProjectStatusTableReference {
        public DirectReferenceTable projectsTable;
        public PredefinedReferenceTable cteRewardStatus;
        public PredefinedReferenceTable cteProjectsWithStatus;
    }

    private SQLSelect createProjectStatusSQLBuilder() {
        return createProjectStatusSQLBuilder(new ProjectStatusTableReference());
    }

    private SQLSelect createProjectStatusSQLBuilder(
            ProjectStatusTableReference tableReferences,
            boolean useCte
    ) {

        DirectReferenceTable projectsTable = new DirectReferenceTable(Tables.PROJECTS);

        DirectReferenceTable rewardsTable = new DirectReferenceTable(Tables.REWARDS);
        DirectReferenceTable cteProjects = new DirectReferenceTable(Tables.PROJECTS);

        PredefinedReferenceTable cteRewardStatus = new PredefinedReferenceTable(createRewardStatusSQLBuilder());


        ValueExpressionToken progressExpression = new ValueExpressionToken(
                new BuiltinFunctionToken(BuiltinFunctions.SUM,
                        new BuiltinFunctionToken(
                                BuiltinFunctions.IFNULL,
                                new TableRefColumnToken(cteRewardStatus, Columns.REWARD_SOLD_PRICE),
                                new UserValueToken(0)
                        )
                ),
                ArithmeticOperator.DIV,
                new TableRefColumnToken(cteProjects, ProjectsDAO.Columns.GOAL)
        );

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
                                        progressExpression
                                ),
                                new ColumnFromTokenSelector(
                                        Columns.PROJECT_FUND_STATUS,
                                        new BuiltinFunctionToken(
                                                BuiltinFunctions.IF,
                                                new ValueConditionToken(
                                                        createEndedCondition(true)
                                                ),
                                                new BuiltinFunctionToken(
                                                        BuiltinFunctions.IF,
                                                        new ValueConditionToken(
                                                                new BasicCondition(
                                                                        progressExpression,
                                                                        ArithmeticConditionOperator.GE,
                                                                        new UserValueToken(1)
                                                                )
                                                        ),
                                                        new UserValueToken(ProjectFundingStatus.SUCCESS),
                                                        new UserValueToken(ProjectFundingStatus.FAILED)
                                                ),
                                                new UserValueToken(ProjectFundingStatus.ONGOING)
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

        tableReferences.projectsTable = projectsTable;
        tableReferences.cteRewardStatus = cteRewardStatus;
        tableReferences.cteProjectsWithStatus = cteProjectsWithStatus;

        PredefinedReferenceTable[] cte;

        if (useCte) {
            cte = new PredefinedReferenceTable[]{
                    cteRewardStatus, cteProjectsWithStatus
            };
        } else {
            cte = new PredefinedReferenceTable[0];
        }

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
                new ColumnFromTokenSelector(Columns.PROJECT_FUND_STATUS, new TableRefColumnToken(cteProjectsWithStatus, Columns.PROJECT_FUND_STATUS)),
        }, new JoinedTable(
                projectsTable,
                JoinType.LEFT,
                rewardsTable,
                new BasicCondition(
                        new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.PROJECT_ID)
                )
        ), cte);
    }

    private SQLSelect createCategoryFilteredProjectStatusSQLBuilder(Category category) {
        ProjectStatusTableReference reference = new ProjectStatusTableReference();
        SQLSelect select = createProjectStatusSQLBuilder(reference, false);
        DirectReferenceTable categoryTable = new DirectReferenceTable(Tables.CATEGORY);
        DirectReferenceTable projectsTable = reference.projectsTable;
        PredefinedReferenceTable cteRewardStatus = reference.cteRewardStatus;
        PredefinedReferenceTable cteProjectsWithStatus = reference.cteProjectsWithStatus;

        return new SQLSelect(
                new ColumnSelector[]{
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
                        new ColumnFromTokenSelector(Columns.PROJECT_FUND_STATUS, new TableRefColumnToken(cteProjectsWithStatus, Columns.PROJECT_FUND_STATUS)),
                },
                new JoinedTable(new InlineViewReferenceTable(select), JoinType.LEFT, categoryTable,
                        Condition.and(new BasicCondition(
                                        new TableRefColumnToken(categoryTable, CategoryDAO.Columns.PROJECT_ID),
                                        ArithmeticConditionOperator.EQ,
                                        new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID)
                                ), new BasicCondition(
                                        new TableRefColumnToken(categoryTable, CategoryDAO.Columns.CATEGORY_NAME),
                                        ArithmeticConditionOperator.EQ,
                                        new UserValueToken(category.toString())
                                )
                        )), cteRewardStatus, cteProjectsWithStatus
        );
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================


    public List<ProjectQueryDTO> getProject(Connection conn, long projectId) throws SQLException {

        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(new BasicCondition(new ColumnValueToken(ProjectsDAO.Columns.PROJECT_ID), ArithmeticConditionOperator.EQ, new UserValueToken(projectId)))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    public final List<ProjectQueryDTO> getActiveProjects(Connection conn, SimpleOrderBySelector... sorter) throws SQLException {
        return getActiveProjectsForName(conn, "", sorter);
    }

    public List<ProjectQueryDTO> getActiveProjectsForName(Connection conn, String projectName, SimpleOrderBySelector... sorter) throws SQLException {
        return queryActiveProjects(conn, projectName, sorter);
    }

    public final List<ProjectQueryDTO> getFilteredActiveProjects(Connection conn, Category category, SimpleOrderBySelector... sorter) throws SQLException {
        return getFilteredActiveProjectsForName(conn, "", category, sorter);
    }

    public List<ProjectQueryDTO> getFilteredActiveProjectsForName(Connection conn, String projectName, Category category, SimpleOrderBySelector... sorter) throws SQLException {
        return queryFilteredActiveProjects(conn, projectName, category, sorter);
    }

    public final List<ProjectQueryDTO> getFilteredEndedProjects(Connection conn, Category category, SimpleOrderBySelector... sorter) throws SQLException {
        return getFilteredEndedProjectsForName(conn, "", category, sorter);
    }

    public List<ProjectQueryDTO> getFilteredEndedProjectsForName(Connection conn, String projectName, Category category, SimpleOrderBySelector... sorter) throws SQLException {
        return queryFilteredEndedProjects(conn, projectName, category, sorter);
    }

    public final List<ProjectQueryDTO> getFailedProjects(Connection conn, SimpleOrderBySelector... sorter) throws SQLException {
        return getFailedProjectsForName(conn, "", sorter);
    }

    public List<ProjectQueryDTO> getFailedProjectsForName(Connection conn, String projectName, SimpleOrderBySelector... sorter) throws SQLException {
        return queryFailedProjects(conn, projectName, sorter);
    }

    public final List<ProjectQueryDTO> getMyProjects(Connection conn, long userId, ApprovalStatus approvalStatus, ProjectFundingStatus fundingStatus, SimpleOrderBySelector... sorter) throws SQLException {
        return getMyProjectsForName(conn, userId, "", approvalStatus, fundingStatus, sorter);
    }

    public final List<ProjectQueryDTO> getMyProjectsForName(Connection conn, long userId, String projectName, ApprovalStatus approvalStatus, ProjectFundingStatus fundingStatus, SimpleOrderBySelector... sorter) throws SQLException {
        return queryMyProjects(conn, userId, projectName, approvalStatus, fundingStatus, sorter);
    }

    public final List<ProjectQueryDTO> getPendingProjects(Connection conn, SimpleOrderBySelector... sorter) throws SQLException {
        return getPendingProjectsForName(conn, "", sorter);
    }

    public List<ProjectQueryDTO> getPendingProjectsForName(Connection conn, String projectName, SimpleOrderBySelector... sorter) throws SQLException {
        return queryPendingProjects(conn, projectName, sorter);
    }

    public final List<FundDTO> getSupportStatus(Connection conn, long projectId, SimpleOrderBySelector... sorter) throws SQLException {
        return querySupportStatus(conn, projectId, sorter);
    }

    public final List<ProjectQueryDTO> getAllProjects(Connection conn, SimpleOrderBySelector... sorter) throws SQLException {
        return getAllProjectsForName(conn, "", sorter);
    }

    public List<ProjectQueryDTO> getAllProjectsForName(Connection conn, String projectName, SimpleOrderBySelector... sorter) throws SQLException {
        return queryAllProjects(conn, projectName, sorter);
    }

    public final List<ProjectQueryDTO> getSuccessProjects(Connection conn, SimpleOrderBySelector... sorter) throws SQLException {
        return getSuccessProjectsForName(conn, "", sorter);
    }

    public List<ProjectQueryDTO> getSuccessProjectsForName(Connection conn, String projectName, SimpleOrderBySelector... sorter) throws SQLException {
        return querySuccessProjects(conn, projectName, sorter);
    }

    public final List<ProjectQueryDTO> getRejectedProjects(Connection conn, SimpleOrderBySelector... sorter) throws SQLException {
        return getActiveProjectsForName(conn, "", sorter);
    }

    public List<ProjectQueryDTO> getRejectedProjectsForName(Connection conn, String projectName, SimpleOrderBySelector... sorter) throws SQLException {
        return queryRejectedProjects(conn, projectName, sorter);
    }

    public final List<ProjectQueryDTO> getEndedProjects(Connection conn, SimpleOrderBySelector... sorter) throws SQLException {
        return getActiveProjectsForName(conn, "", sorter);
    }

    public List<ProjectQueryDTO> getEndedProjectsForName(Connection conn, String projectName, SimpleOrderBySelector... sorter) throws SQLException {
        return queryEndedProjects(conn, projectName, sorter);
    }

    private List<ProjectQueryDTO> queryActiveProjects(Connection conn, String projectName, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(createEndedCondition(false))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    private List<ProjectQueryDTO> queryFailedProjects(Connection conn, String projectName, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(createEndedCondition(true))
                .addCondition(createProjectProgressCondition(false))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    private List<ProjectQueryDTO> querySuccessProjects(Connection conn, String projectName, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(createEndedCondition(true))
                .addCondition(createProjectProgressCondition(true))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    private List<FundDTO> querySupportStatus(Connection connection, long projectId, OrderBySelector... sorter) throws SQLException {
        List<FundDTO> result = new ArrayList<>();
        try (ResultSet rs = SQLSelect.allColumns(Tables.FUNDS)
                .addCondition(new BasicCondition(
                        new ColumnValueToken(FundsDAO.Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)
                ))
                .orderBy(sorter)
                .execute(connection)) {
            while (rs.next()) {
                result.add(FundsDAO.toDTO(rs));
            }
        }
        return result;
    }

    private List<ProjectQueryDTO> queryFilteredActiveProjects(Connection conn, String projectName, Category category, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createCategoryFilteredProjectStatusSQLBuilder(category)
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(createEndedCondition(false))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    private List<ProjectQueryDTO> queryFilteredEndedProjects(Connection conn, String projectName, Category category, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createCategoryFilteredProjectStatusSQLBuilder(category)
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(createEndedCondition(true))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    private List<ProjectQueryDTO> queryEndedProjects(Connection conn, String projectName, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.APPROVED))
                .addCondition(createEndedCondition(true))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    private List<ProjectQueryDTO> queryPendingProjects(Connection conn, String projectName, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.PENDING))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    private List<ProjectQueryDTO> queryRejectedProjects(Connection conn, String projectName, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(createProjectApprovalStatusCondition(ApprovalStatus.REJECTED))
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }


    private List<ProjectQueryDTO> queryMyProjects(Connection conn, long userId, String projectName, ApprovalStatus approvalStatus, ProjectFundingStatus fundingStatus, SimpleOrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        SQLSelect select = createProjectStatusSQLBuilder().addCondition(createTitlePatternCondition(projectName));

        if (approvalStatus != null)
            select.addCondition(createProjectApprovalStatusCondition(approvalStatus));
        if (fundingStatus != null)
            select.addCondition(createProjectFundingStatusCondition(fundingStatus));

        try (ResultSet rs = select
                .addCondition(
                        new BasicCondition(
                                new ColumnValueToken(ProjectsDAO.Columns.USER_ID),
                                ArithmeticConditionOperator.EQ,
                                new UserValueToken(userId)
                        )
                )
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    private List<ProjectQueryDTO> queryAllProjects(Connection conn, String projectName, OrderBySelector... sorter) throws SQLException {
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createProjectStatusSQLBuilder()
                .addCondition(createTitlePatternCondition(projectName))
                .orderBy(sorter)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs));
            }
        }
        return result;
    }

    private BasicCondition createProjectApprovalStatusCondition(ApprovalStatus approvalStatus) {
        return new BasicCondition(
                new ColumnValueToken(ProjectsDAO.Columns.APPROVAL_STATUS),
                ArithmeticConditionOperator.EQ,
                new UserValueToken(approvalStatus)
        );
    }


    private BasicCondition createProjectFundingStatusCondition(ProjectFundingStatus fundingStatus) {
        return new BasicCondition(
                new ColumnValueToken(Columns.PROJECT_FUND_STATUS),
                ArithmeticConditionOperator.EQ,
                new UserValueToken(fundingStatus)
        );
    }

    private BasicCondition createEndedCondition(boolean isEnd) {
        return new BasicCondition(
                new ColumnValueToken(ProjectsDAO.Columns.END_DATE),
                isEnd ? ArithmeticConditionOperator.LE : ArithmeticConditionOperator.GT,
                new UserValueToken(
                        DateTimeUtil.now()
                )
        );
    }

    private BasicCondition createTitlePatternCondition(String projectName) {
        return new BasicCondition(
                new ColumnValueToken(ProjectsDAO.Columns.TITLE),
                SetConditionOperator.LIKE,
                new UserValueToken(projectName.isEmpty() ? "%" : "%" + projectName + "%"));
    }

    private BasicCondition createProjectProgressCondition(boolean isSuccess) {
        return new BasicCondition(
                new ColumnValueToken(Columns.PROJECT_FUND_PROGRESS),
                isSuccess ? ArithmeticConditionOperator.GE : ArithmeticConditionOperator.LT,
                new UserValueToken(1)
        );
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
        dto.setProjectFundingStatus(ProjectFundingStatus.valueOf(rs.getString(Columns.PROJECT_FUND_STATUS.toString())));
        return dto;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================


    // =================================================================
    // 4. DELETE SECTION
    // =================================================================


}
