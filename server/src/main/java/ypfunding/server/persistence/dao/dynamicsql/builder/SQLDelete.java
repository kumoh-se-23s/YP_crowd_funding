package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;

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
        return PredefinedReferenceTable.makeQuery(predefined) + " delete from " + referenceTable + Condition.whereChain(conditions);
    }

    @Override
    public Integer execute(Connection conn) throws SQLException {
        SQLLogger.logQuery(this);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, PredefinedReferenceTable.makeQuery(predefined) + " " + this);
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }
}
