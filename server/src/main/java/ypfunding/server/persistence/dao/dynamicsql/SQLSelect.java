package ypfunding.server.persistence.dao.dynamicsql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class SQLSelect extends SQLBuilderImpl {
    private final List<ColumnSelector> columnSelectors;
    private final ReferenceTable referenceTable;

    public SQLSelect(Connection conn, List<ColumnSelector> columnSelectors, ReferenceTable referenceTable) {
        super(conn);
        this.columnSelectors = columnSelectors;
        this.userParameters.addAll(columnSelectors.stream().map(ColumnSelector::getUserParameters).toList());

        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());

    }

    public SQLSelect addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }


    @Override
    public void execute() throws SQLException {
        String sql = "select " + ColumnSelector.buildColumnParentheses(columnSelectors) +
                     " from " +
                     referenceTable + Condition.whereChain(conditions);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sql);
        preparedStatement.addParams(userParameters);
        preparedStatement.applyParam().executeUpdate();
    }
}
