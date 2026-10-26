package ypfunding.server.persistence.dao.dynamicsql.builder;

import lombok.Getter;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract sealed class SQLBuilder<T> permits
        SQLDelete,
        SQLInsert,
        SQLSelect,
        SQLUpdate {
    protected final List<Condition> conditions = new ArrayList<>();
    protected final List<Object> userParameters = new ArrayList<>();
    @Getter
    protected final PredefinedReferenceTable[] predefined;

    protected SQLBuilder(PredefinedReferenceTable... predefined) {
        this.predefined = predefined;
        userParameters.addAll(Arrays.stream(predefined).map(PredefinedReferenceTable::getUserParameters).flatMap(List::stream).toList());
    }

    protected void addConditionImpl(Condition condition) {
        conditions.add(condition);
        userParameters.addAll(condition.getUserParameters());
    }

    public List<Object> getUserParameters() {
        return Collections.unmodifiableList(userParameters);
    }

    public abstract T execute(Connection conn) throws SQLException;
}
