package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.ReferenceTable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SQLSelect extends SQLBuilder<ResultSet> {
    private final List<ColumnSelector> columnSelectors;
    private final ReferenceTable referenceTable;

    public SQLSelect(List<ColumnSelector> columnSelectors, ReferenceTable referenceTable) {
        this.columnSelectors = columnSelectors;
        this.userParameters.addAll(columnSelectors.stream().map(ColumnSelector::getUserParameters).flatMap(List::stream).toList());

        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());

    }

    public static SQLSelect allColumns(ReferenceTable referenceTable){
        return new SQLSelect(List.of(), referenceTable);
    }

    public static SQLSelect allColumns(Tables table){
        return allColumns(new DirectReferenceTable(table));
    }


    public SQLSelect addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }


    @Override
    public String toString() {
        return "select " + (columnSelectors.isEmpty() ? "*" : ColumnSelector.buildSelectColumnParentheses(columnSelectors)) +
               " from " +
               referenceTable + Condition.whereChain(conditions);
    }

    @Override
    public ResultSet execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeQuery();
    }
}
