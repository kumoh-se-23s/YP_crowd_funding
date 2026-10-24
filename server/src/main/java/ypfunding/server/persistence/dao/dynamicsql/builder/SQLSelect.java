package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.OrderBySelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.ReferenceTable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class SQLSelect extends SQLBuilder<ResultSet> {
    private final ColumnSelector[] columnSelectors;
    private final ReferenceTable referenceTable;
    private final List<OrderBySelector> orderBySelectors = new ArrayList<>();

    public SQLSelect(ColumnSelector columnSelector, ReferenceTable referenceTable, PredefinedReferenceTable... predefined) {
        this(new ColumnSelector[]{columnSelector}, referenceTable, predefined);
    }

    public SQLSelect(ColumnSelector[] columnSelectors, ReferenceTable referenceTable, PredefinedReferenceTable... predefined) {
        super(predefined);
        this.columnSelectors = columnSelectors;
        this.userParameters.addAll(Arrays.stream(columnSelectors).map(ColumnSelector::getUserParameters).flatMap(List::stream).toList());

        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());

    }

    public static SQLSelect allColumns(ReferenceTable referenceTable){
        return new SQLSelect(new ColumnSelector[0], referenceTable);
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
        return "select " + (columnSelectors.length == 0 ? "*" : ColumnSelector.buildSelectColumnParentheses(columnSelectors)) +
               " from " +
               referenceTable + Condition.whereChain(conditions) +
               orderBySelectors.stream().map(OrderBySelector::toString).collect(Collectors.joining(" "));
    }

    public SQLSelect orderBy(OrderBySelector token){
        this.orderBySelectors.add(token);
        this.userParameters.addAll(token.getUserParameters());
        return this;
    }

    @Override
    public ResultSet execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn,
                PredefinedReferenceTable.makeQuery(predefined) + " " + this);
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeQuery();
    }
}
