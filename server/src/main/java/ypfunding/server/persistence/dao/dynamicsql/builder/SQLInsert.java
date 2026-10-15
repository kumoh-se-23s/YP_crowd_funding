package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SQLInsert extends SQLBuilder<Integer> {

    private final Tables table;
    private final List<ColumnsEnum> columns = new ArrayList<>();

    public SQLInsert(Tables table, PredefinedReferenceTable... predefined) {
        super(predefined);
        this.table = table;
    }


    public SQLInsert addTarget(ColumnsEnum selector, Object value) {
        columns.add(selector);
        userParameters.add(value);
        return this;
    }

    @Override
    public String toString() {
        return PredefinedReferenceTable.makeQuery(predefined) + " insert into " + table +
               ColumnSelector.buildColumnParenthesesWithValues(columns);
    }

    @Override
    public Integer execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, PredefinedReferenceTable.makeQuery(predefined) + " " + this);
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }
}
