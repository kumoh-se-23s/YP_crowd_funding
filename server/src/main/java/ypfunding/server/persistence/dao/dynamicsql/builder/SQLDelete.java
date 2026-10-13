package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;

public final class SQLDelete extends SQLBuilder<Integer> {

    private final DirectReferenceTable referenceTable;

    public SQLDelete(DirectReferenceTable referenceTable) {
        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());
    }

    public SQLDelete addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }

    @Override
    public String toString() {
        return "delete from " + referenceTable + Condition.whereChain(conditions);
    }

    @Override
    public Integer execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }
}
