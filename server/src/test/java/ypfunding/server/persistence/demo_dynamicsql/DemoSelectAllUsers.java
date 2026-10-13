package ypfunding.server.persistence.demo_dynamicsql;

import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;

public class DemoSelectAllUsers implements DemoSQL{
    @Override
    public void execute(Connection conn) throws SQLException {
        SQLSelect.allColumns(conn, new DirectReferenceTable(Tables.USERS)).execute();
    }
}
