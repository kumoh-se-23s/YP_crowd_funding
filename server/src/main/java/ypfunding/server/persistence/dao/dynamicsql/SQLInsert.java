package ypfunding.server.persistence.dao.dynamicsql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SQLInsert extends SQLBuilderImpl {

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
    public void execute() throws SQLException {
        String sql = "insert into " + referenceTable +
                     ColumnSelector.buildColumnParenthesesWithWildcard(columns);

        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sql);
        preparedStatement.addParams(userParameters);
        preparedStatement.applyParam().executeQuery();
    }
}
