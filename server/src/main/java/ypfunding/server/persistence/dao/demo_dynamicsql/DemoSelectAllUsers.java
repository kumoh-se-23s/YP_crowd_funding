package ypfunding.server.persistence.dao.demo_dynamicsql;

import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLBuilder;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DemoSelectAllUsers implements DemoSQL{
    @Override
    public void execute(Connection conn) throws SQLException {
        SQLSelect.allColumns(conn, new DirectReferenceTable(Tables.USERS)).execute();
    }
}
