package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class SQLDelete extends SQLBuilder<Integer> {

    private final DirectReferenceTable referenceTable;

    public SQLDelete(Tables tableName, PredefinedReferenceTable... predefined) {
        super(predefined);
        this.referenceTable = new DirectReferenceTable(tableName);
        this.userParameters.addAll(referenceTable.getUserParameters());
    }

    public SQLDelete addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }

    @Override
    public String toString() {
        String cteStr = predefined.length == 0 ? "" : PredefinedReferenceTable.makeQuery(predefined) + "\n";
        String whereStr = conditions.isEmpty() ? "" : "where " + Condition.and(conditions);
        return cteStr + "delete from " + referenceTable + "\n" + whereStr;
    }

    @Override
    public Integer execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }
}
