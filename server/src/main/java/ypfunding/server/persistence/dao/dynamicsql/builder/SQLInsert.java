package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.log.Severity;
import ypfunding.server.persistence.dao.dynamicsql.table.ReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SQLInsert extends SQLBuilder<Integer> {

    private final ReferenceTable referenceTable;
    private final List<ColumnSelector> columns = new ArrayList<>();

    public SQLInsert(Connection conn, ReferenceTable referenceTable) {
        super(conn);
        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());
    }


    public SQLInsert addColumn(ColumnSelector selector) {
        columns.add(selector);
        userParameters.addAll(selector.getUserParameters());
        return this;
    }

    @Override
    public String toString() {
        return "insert into " + referenceTable +
               ColumnSelector.buildColumnParenthesesWithWildcard(columns);
    }

    @Override
    public Integer execute() throws SQLException {
        SQLLogger.log(Severity.INFO, toString());
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }
}
