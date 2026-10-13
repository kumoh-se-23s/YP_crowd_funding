package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.log.Severity;
import ypfunding.server.persistence.dao.dynamicsql.table.ReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;

public final class SQLDelete extends SQLBuilder<Integer> {

    private final ReferenceTable referenceTable;

    public SQLDelete(Connection conn, ReferenceTable referenceTable) {
        super(conn);
        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());
    }

    public SQLDelete addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }

    @Override
    public String toString() {
        return "delete " + referenceTable + Condition.whereChain(conditions);
    }

    @Override
    public Integer execute() throws SQLException {
        SQLLogger.log(Severity.INFO, toString());
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }
}
