package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.UpdateSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class SQLUpdate extends SQLBuilder<Integer> {

    private final DirectReferenceTable referenceTable;
    private final List<ColumnsEnum> columns = new ArrayList<>();

    public SQLUpdate(Tables referenceTable, UpdateSelector[] setters, PredefinedReferenceTable... predefined) {
        this(new DirectReferenceTable(referenceTable), setters, predefined);
    }

    public SQLUpdate(DirectReferenceTable referenceTable, UpdateSelector[] setters, PredefinedReferenceTable... predefined) {
        super(predefined);
        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());

        columns.addAll(Arrays.stream(setters).map(UpdateSelector::column).toList());
        for (UpdateSelector setter : setters) {
            userParameters.addAll(setter.token().getUserParameters());
        }
    }

    public SQLUpdate addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }

    @Override
    public Integer execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }

    @Override
    public String toString() {
        String cteStr = predefined.length == 0 ? "" : PredefinedReferenceTable.makeQuery(predefined) + "\n";
        String whereStr = conditions.isEmpty() ? "" : "where " + Condition.and(conditions) + "\n";
        String setValueQuery = columns.stream().map(e -> e + "=?").collect(Collectors.joining(","));
        return cteStr + "update " + referenceTable +
               "\nset " + setValueQuery +
               "\n" + whereStr;
    }
}
