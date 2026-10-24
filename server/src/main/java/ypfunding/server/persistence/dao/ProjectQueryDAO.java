package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.col.*;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinType;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinedTable;
import ypfunding.server.persistence.dao.dynamicsql.token.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjectQueryDAO extends DAO {
    private final RewardsDAO rewardsDAO;
    private final FundsDAO fundsDAO;

    private enum Columns implements ColumnsEnum{
        TOTAL("total"),
        REMAINING_STOCK("remaining_stock");

        private String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public ProjectQueryDAO(RewardsDAO rewardsDAO, FundsDAO fundsDAO) {
        this.rewardsDAO = rewardsDAO;
        this.fundsDAO = fundsDAO;
    }


    // =================================================================
    // 1. CREATE SECTION
    // =================================================================



    // =================================================================
    // 2. READ SECTION
    // =================================================================

    private SQLSelect createFundingStatusSQLBuilder(DirectReferenceTable projectsTable, DirectReferenceTable rewardsTable) {
        return new SQLSelect(new ColumnSelector[]{
                new ColumnFromTokenSelector(new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID)),
                new ColumnFromTokenSelector(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.REWARD_ID)),
                new ColumnFromTokenSelector(new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.TITLE)),
                new ColumnFromTokenSelector(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.NAME)),
                new ColumnFromTokenSelector(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.DESCRIPTION)),
                new ColumnFromTokenSelector(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.PRICE)),
                new ColumnFromTokenSelector(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.STOCK)),
                new ColumnFromTokenSelector(Columns.TOTAL, new ValueExpressionToken(new ColumnValueToken(RewardsDAO.Columns.PRICE), ArithmeticOperator.MUL, new ColumnValueToken(RewardsDAO.Columns.STOCK))),
                new ColumnFromTokenSelector(Columns.REMAINING_STOCK, new ValueExpressionToken(
                        new ColumnValueToken(RewardsDAO.Columns.STOCK),
                        ArithmeticOperator.SUB,
                        new SubqueryToken(
                                new SQLSelect(
                                        new ColumnSelector[]{
                                                new ColumnFromTokenSelector(
                                                        new BuiltinFunctionToken(
                                                                BuiltinFunctions.SUM,
                                                                new ColumnValueToken(FundsDAO.Columns.QUANTITY)
                                                        )
                                                )
                                        },
                                        new DirectReferenceTable(Tables.FUNDS))
                                        .addCondition(
                                                new BasicCondition(
                                                        new ColumnValueToken(FundsDAO.Columns.REWARD_ID),
                                                        ArithmeticConditionOperator.EQ,
                                                        new ColumnValueToken(RewardsDAO.Columns.REWARD_ID)
                                                )
                                        ))
                ))
        }, new JoinedTable(
                projectsTable,
                JoinType.LEFT,
                rewardsTable,
                new BasicCondition(
                        new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.PROJECT_ID)
                )
        ));
    }

    // Displayed projects with funding status ()
    // Admin only.
    public List<ProjectQueryDTO> getProjectWithFundingStatus(Connection conn, long projectID) throws SQLException {

        DirectReferenceTable projectsTable = new DirectReferenceTable(Tables.PROJECTS);
        DirectReferenceTable rewardsTable = new DirectReferenceTable(Tables.REWARDS);
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createFundingStatusSQLBuilder(projectsTable, rewardsTable)
                .addCondition(new BasicCondition(new ColumnValueToken(RewardsDAO.Columns.PROJECT_ID), ArithmeticConditionOperator.EQ, new UserValueToken(projectID)))
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs, projectsTable, rewardsTable));
            }
        }
        return result;
    }

    // Displayed projects with funding status ()
    // Admin only.
    public List<ProjectQueryDTO> getDisplayedProjectsWithFundingStatus(Connection conn) throws SQLException {

        DirectReferenceTable projectsTable = new DirectReferenceTable(Tables.PROJECTS);
        DirectReferenceTable rewardsTable = new DirectReferenceTable(Tables.REWARDS);
        List<ProjectQueryDTO> result = new ArrayList<>();
        try (ResultSet rs = createFundingStatusSQLBuilder(projectsTable, rewardsTable)
                .execute(conn)) {
            while (rs.next()) {
                result.add(toDTO(rs, projectsTable, rewardsTable));
            }
        }
        return result;
    }

    private ProjectQueryDTO toDTO(ResultSet rs, DirectReferenceTable projectsTable, DirectReferenceTable rewardsTable) throws SQLException{
        ProjectQueryDTO dto = new ProjectQueryDTO();
        dto.setProjectId(rs.getLong(new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.PROJECT_ID).toString()));
        dto.setRewardId(rs.getLong(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.REWARD_ID).toString()));
        dto.setProjectTitle(rs.getString(new TableRefColumnToken(projectsTable, ProjectsDAO.Columns.TITLE).toString()));
        dto.setRewardName(rs.getString(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.NAME).toString()));
        dto.setRewardDescription(rs.getString(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.DESCRIPTION).toString()));
        dto.setPrice(rs.getLong(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.PRICE).toString()));
        dto.setTotalStock(rs.getLong(new TableRefColumnToken(rewardsTable, RewardsDAO.Columns.STOCK).toString()));
        dto.setTotalPrice(rs.getLong(Columns.TOTAL.toString()));
        dto.setRemainingStock(rs.getLong(Columns.REMAINING_STOCK.toString()));
        return dto;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================


    // =================================================================
    // 4. DELETE SECTION
    // =================================================================


}
