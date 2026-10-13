package ypfunding.server.persistence.demo_dynamicsql;

import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.UsersDAO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;

import java.sql.Connection;
import java.sql.SQLException;

public class DemoSelectFindUsersByName implements DemoSQL {

    private final String username;

    public DemoSelectFindUsersByName(String username) {
        this.username = username;
    }

    @Override
    public void execute(Connection conn) throws SQLException {
        SQLSelect.allColumns(conn, new DirectReferenceTable(Tables.USERS))
                .addCondition(
                        new BasicCondition(
                                UsersDAO.Columns.NAME,
                                ArithmeticConditionOperator.EQ,
                                new UserValueToken(username)
                        )
                )
                .execute();
    }
}
