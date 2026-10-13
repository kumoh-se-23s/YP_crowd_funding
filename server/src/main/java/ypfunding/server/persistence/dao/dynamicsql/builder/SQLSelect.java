package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.log.Severity;
import ypfunding.server.persistence.dao.dynamicsql.table.ReferenceTable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SQLSelect extends SQLBuilder<ResultSet> {
    private final List<ColumnSelector> columnSelectors;
    private final ReferenceTable referenceTable;

    public SQLSelect(Connection conn, List<ColumnSelector> columnSelectors, ReferenceTable referenceTable) {
        super(conn);
        this.columnSelectors = columnSelectors;
        this.userParameters.addAll(columnSelectors.stream().map(ColumnSelector::getUserParameters).toList());

        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());

    }

    public static SQLSelect allColumns(Connection conn, ReferenceTable referenceTable){
        return new SQLSelect(conn, List.of(), referenceTable);
    }

    public SQLSelect addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }

    @Override
    public String toString() {
        return "select " + (columnSelectors.isEmpty() ? "*" : ColumnSelector.buildColumnParentheses(columnSelectors)) +
        " from " +
        referenceTable + Condition.whereChain(conditions);
    }

    @Override
    public ResultSet execute() throws SQLException {
        SQLLogger.log(Severity.INFO, toString());
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeQuery();
    }
}
