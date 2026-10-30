package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnFromTokenSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.DirectColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.OrderBySelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.ReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.TableRefColumnToken;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SQLSelect extends SQLBuilder<ResultSet> {
    private final ColumnSelector[] columnSelectors;
    private final ReferenceTable referenceTable;
    private final List<OrderBySelector> orderBySelectors = new ArrayList<>();
    private ColumnSelector[] groupByColumns = null;

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

    public static SQLSelect allColumns(ReferenceTable referenceTable) {
        return new SQLSelect(new ColumnSelector[0], referenceTable);
    }

    public static SQLSelect allColumns(Tables table) {
        return allColumns(new DirectReferenceTable(table));
    }


    public SQLSelect addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }


    @Override
    public String toString() {
        String cteStr = predefined.length == 0 ? "" : PredefinedReferenceTable.makeQuery(predefined) + "\n";
        String whereStr = conditions.isEmpty() ? "" : "where " + Condition.and(conditions) + "\n";
        String groupByStr = (groupByColumns == null ? "" : "group by " + Arrays.stream(groupByColumns).map(ColumnSelector::toString).collect(Collectors.joining(", "))) + "\n";
        String orderByStr = orderBySelectors.isEmpty() ? "" : "order by " + orderBySelectors.stream().map(OrderBySelector::toString).collect(Collectors.joining(", "));
        return cteStr + "select " + (columnSelectors.length == 0 ? "*" : ColumnSelector.buildSelectColumnParentheses(columnSelectors)) +
               "\nfrom " +
               referenceTable + "\n" + whereStr + groupByStr + orderByStr;
    }

    public SQLSelect orderBy(OrderBySelector token) {
        this.orderBySelectors.add(token);
        this.userParameters.addAll(token.getUserParameters());
        return this;
    }

    public SQLSelect orderBy(OrderBySelector... token) {
        this.orderBySelectors.addAll(List.of(token));
        this.userParameters.addAll(Stream.of(token).flatMap(e -> e.getUserParameters().stream()).toList());
        return this;
    }

    @Override
    public ResultSet execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeQuery();
    }

    public SQLSelect groupBy(TableRefColumnToken... columns) {
        if (groupByColumns != null)
            throw new IllegalStateException("Group by already set");
        groupByColumns = Arrays.stream(columns).map(ColumnFromTokenSelector::new).toArray(ColumnSelector[]::new);
        return this;
    }

    public SQLSelect groupBy(ColumnsEnum... columns) {
        if (groupByColumns != null)
            throw new IllegalStateException("Group by already set");
        groupByColumns = Arrays.stream(columns).map(DirectColumnSelector::new).toArray(ColumnSelector[]::new);
        return this;
    }
}
