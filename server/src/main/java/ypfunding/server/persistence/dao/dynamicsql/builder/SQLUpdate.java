package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.AccumulativePreparedStatement;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.log.SQLLogger;
import ypfunding.server.persistence.dao.dynamicsql.log.Severity;
import ypfunding.server.persistence.dao.dynamicsql.table.ReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.col.UpdateSelector;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class SQLUpdate extends SQLBuilder<Integer> {

    private final ReferenceTable referenceTable;
    private final List<ColumnsEnum> columns = new ArrayList<>();

    public SQLUpdate(Connection conn, ReferenceTable referenceTable, UpdateSelector... setters) {
        super(conn);

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
    public String toString() {
        String setValueQuery = columns.stream().map(e -> e + "=?").collect(Collectors.joining(","));
        return "update " +
                            referenceTable +
                            " set " +
                            setValueQuery +
                            Condition.whereChain(conditions);
    }

    @Override
    public Integer execute() throws SQLException {
        SQLLogger.log(Severity.INFO, toString());
        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, toString());
        preparedStatement.addParams(userParameters);
        return preparedStatement.applyParam().executeUpdate();
    }
}
