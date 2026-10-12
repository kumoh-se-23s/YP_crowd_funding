package ypfunding.server.persistence.dao.dynamicsql;

import java.sql.Connection;
import java.sql.SQLException;

public final class SQLDelete extends SQLBuilderImpl {

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
    public void execute() throws SQLException {
        String sql = "delete " + referenceTable + Condition.whereChain(conditions);
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sql);
        preparedStatement.addParams(userParameters);
        preparedStatement.applyParam().executeUpdate();

    }
}
