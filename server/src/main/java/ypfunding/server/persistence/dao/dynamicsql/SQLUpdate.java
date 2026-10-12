package ypfunding.server.persistence.dao.dynamicsql;

import ypfunding.server.persistence.dao.ColumnsEnum;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class SQLUpdate extends SQLBuilderImpl {

    private final ReferenceTable referenceTable;
    private final List<ColumnsEnum> columns = new ArrayList<>();

    public SQLUpdate(Connection conn, ReferenceTable referenceTable, UpdateSetter... setters) {
        super(conn);

        this.referenceTable = referenceTable;
        this.userParameters.addAll(referenceTable.getUserParameters());

        columns.addAll(Arrays.stream(setters).map(UpdateSetter::column).toList());
        for (UpdateSetter setter : setters) {
            userParameters.addAll(setter.token().getUserParameters());
        }
    }

    public SQLUpdate addCondition(Condition condition) {
        addConditionImpl(condition);
        return this;
    }


    @Override
    public void execute() throws SQLException {
        String setValueQuery = columns.stream().map(e -> e + "=?").collect(Collectors.joining(","));
        String sqlBuilder = "update " +
                            referenceTable +
                            " set " +
                            setValueQuery +
                            Condition.whereChain(conditions);

        AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sqlBuilder);
        preparedStatement.addParams(userParameters);
        preparedStatement.applyParam().executeUpdate();
    }
}
