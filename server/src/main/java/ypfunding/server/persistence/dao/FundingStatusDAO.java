package ypfunding.server.persistence.dao;

import ypfunding.common.dto.FundingSupporterDTO;
import ypfunding.common.dto.RewardStatusDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnFromTokenSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.GenericOrderBySelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinType;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinedTable;
import ypfunding.server.persistence.dao.dynamicsql.token.BuiltinFunctionToken;
import ypfunding.server.persistence.dao.dynamicsql.token.BuiltinFunctions;
import ypfunding.server.persistence.dao.dynamicsql.token.TableRefColumnToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueExpressionToken;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ypfunding.common.constant.DomainRules.UNLIMITED_STOCK;

public class FundingStatusDAO {
    private enum Columns implements ColumnsEnum {
        APPLIED_QUANTITY("applied_quantity"),
        REWARD_NAME("reward_name"),
        AMOUNT("amount");

        private final String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    //read----------------------------------
    public List<RewardStatusDTO> findRewardStatuses(Connection conn, long projectId) throws SQLException {
        DirectReferenceTable rewards = new DirectReferenceTable(Tables.REWARDS);
        DirectReferenceTable funds = new DirectReferenceTable(Tables.FUNDS);

        SQLSelect select = new SQLSelect(
                new ColumnSelector[]{
                        new ColumnFromTokenSelector(RewardsDAO.Columns.REWARD_ID, new TableRefColumnToken(rewards, RewardsDAO.Columns.REWARD_ID)),
                        new ColumnFromTokenSelector(RewardsDAO.Columns.NAME, new TableRefColumnToken(rewards, RewardsDAO.Columns.NAME)),
                        new ColumnFromTokenSelector(RewardsDAO.Columns.PRICE, new TableRefColumnToken(rewards, RewardsDAO.Columns.PRICE)),
                        new ColumnFromTokenSelector(RewardsDAO.Columns.STOCK, new TableRefColumnToken(rewards, RewardsDAO.Columns.STOCK)),
                        // 후원이 없는 리워드는 SUM이 NULL이므로 0으로
                        new ColumnFromTokenSelector(Columns.APPLIED_QUANTITY,
                                new BuiltinFunctionToken(BuiltinFunctions.IFNULL,
                                        new BuiltinFunctionToken(BuiltinFunctions.SUM, new TableRefColumnToken(funds, FundsDAO.Columns.QUANTITY)),
                                        new UserValueToken(0)))
                },
                new JoinedTable(rewards, JoinType.LEFT, funds,
                        new BasicCondition(
                                new TableRefColumnToken(rewards, RewardsDAO.Columns.REWARD_ID),
                                ArithmeticConditionOperator.EQ,
                                new TableRefColumnToken(funds, FundsDAO.Columns.REWARD_ID)))
        )
                .addCondition(new BasicCondition(
                        new TableRefColumnToken(rewards, RewardsDAO.Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)))
                .groupBy(new TableRefColumnToken(rewards, RewardsDAO.Columns.REWARD_ID))
                .orderBy(new GenericOrderBySelector(new TableRefColumnToken(rewards, RewardsDAO.Columns.REWARD_ID), true));

        List<RewardStatusDTO> result = new ArrayList<>();
        try (ResultSet rs = select.execute(conn)) {
            while (rs.next()) {
                result.add(toRewardStatus(rs));
            }
        }
        return result;
    }

    public List<FundingSupporterDTO> findSupporters(Connection conn, long projectId) throws SQLException {
        DirectReferenceTable funds = new DirectReferenceTable(Tables.FUNDS);
        DirectReferenceTable users = new DirectReferenceTable(Tables.USERS);
        DirectReferenceTable rewards = new DirectReferenceTable(Tables.REWARDS);

        SQLSelect select = new SQLSelect(
                new ColumnSelector[]{
                        new ColumnFromTokenSelector(FundsDAO.Columns.FUND_ID, new TableRefColumnToken(funds, FundsDAO.Columns.FUND_ID)),
                        new ColumnFromTokenSelector(UsersDAO.Columns.NAME, new TableRefColumnToken(users, UsersDAO.Columns.NAME)),
                        new ColumnFromTokenSelector(UsersDAO.Columns.ADDRESS, new TableRefColumnToken(users, UsersDAO.Columns.ADDRESS)),
                        new ColumnFromTokenSelector(FundsDAO.Columns.REWARD_ID, new TableRefColumnToken(funds, FundsDAO.Columns.REWARD_ID)),
                        new ColumnFromTokenSelector(Columns.REWARD_NAME, new TableRefColumnToken(rewards, RewardsDAO.Columns.NAME)),
                        new ColumnFromTokenSelector(FundsDAO.Columns.QUANTITY, new TableRefColumnToken(funds, FundsDAO.Columns.QUANTITY)),
                        // 후원 금액은 따로 저장하지 않고 가격 × 수량으로
                        new ColumnFromTokenSelector(Columns.AMOUNT,
                                new ValueExpressionToken(
                                        new TableRefColumnToken(rewards, RewardsDAO.Columns.PRICE),
                                        ArithmeticOperator.MUL,
                                        new TableRefColumnToken(funds, FundsDAO.Columns.QUANTITY))),
                        new ColumnFromTokenSelector(FundsDAO.Columns.DATE, new TableRefColumnToken(funds, FundsDAO.Columns.DATE))
                },
                new JoinedTable(
                        new JoinedTable(funds, JoinType.INNER, users,
                                new BasicCondition(
                                        new TableRefColumnToken(funds, FundsDAO.Columns.USER_ID),
                                        ArithmeticConditionOperator.EQ,
                                        new TableRefColumnToken(users, UsersDAO.Columns.USER_ID))),
                        JoinType.INNER, rewards,
                        new BasicCondition(
                                new TableRefColumnToken(funds, FundsDAO.Columns.REWARD_ID),
                                ArithmeticConditionOperator.EQ,
                                new TableRefColumnToken(rewards, RewardsDAO.Columns.REWARD_ID)))
        )
                .addCondition(new BasicCondition(
                        new TableRefColumnToken(funds, FundsDAO.Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)))
                .orderBy(
                        new GenericOrderBySelector(new TableRefColumnToken(funds, FundsDAO.Columns.DATE), false),
                        new GenericOrderBySelector(new TableRefColumnToken(funds, FundsDAO.Columns.FUND_ID), false));

        List<FundingSupporterDTO> result = new ArrayList<>();
        try (ResultSet rs = select.execute(conn)) {
            while (rs.next()) {
                result.add(toSupporter(rs));
            }
        }
        return result;
    }

    //매핑------------------------------------
    private static RewardStatusDTO toRewardStatus(ResultSet rs) throws SQLException {
        long stock = rs.getLong(RewardsDAO.Columns.STOCK.toString());
        long applied = rs.getLong(Columns.APPLIED_QUANTITY.toString());
        boolean unlimited = stock == UNLIMITED_STOCK;

        RewardStatusDTO dto = new RewardStatusDTO();
        dto.setRewardId(rs.getLong(RewardsDAO.Columns.REWARD_ID.toString()));
        dto.setName(rs.getString(RewardsDAO.Columns.NAME.toString()));
        dto.setPrice(rs.getLong(RewardsDAO.Columns.PRICE.toString()));
        dto.setStock(stock);
        dto.setAppliedQuantity(applied);
        dto.setRemainingQuantity(unlimited ? null : stock - applied);
        dto.setUnlimited(unlimited);
        return dto;
    }

    private static FundingSupporterDTO toSupporter(ResultSet rs) throws SQLException {
        FundingSupporterDTO dto = new FundingSupporterDTO();
        dto.setFundId(rs.getLong(FundsDAO.Columns.FUND_ID.toString()));
        dto.setName(rs.getString(UsersDAO.Columns.NAME.toString()));
        dto.setAddress(rs.getString(UsersDAO.Columns.ADDRESS.toString()));
        dto.setRewardId(rs.getLong(FundsDAO.Columns.REWARD_ID.toString()));
        dto.setRewardName(rs.getString(Columns.REWARD_NAME.toString()));
        dto.setQuantity(rs.getLong(FundsDAO.Columns.QUANTITY.toString()));
        dto.setAmount(rs.getLong(Columns.AMOUNT.toString()));
        dto.setDate(rs.getObject(FundsDAO.Columns.DATE.toString(), LocalDateTime.class));
        return dto;
    }
}
