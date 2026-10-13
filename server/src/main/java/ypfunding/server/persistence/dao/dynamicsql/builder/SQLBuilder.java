package ypfunding.server.persistence.dao.dynamicsql.builder;

import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract sealed class SQLBuilder<T> permits SQLDelete, SQLInsert, SQLSelect, SQLUpdate {

    protected final Connection conn;
    protected final List<Condition> conditions = new ArrayList<>();
    protected final List<Object> userParameters = new ArrayList<>();

    protected SQLBuilder(Connection conn) {
        this.conn = conn;
    }

    protected void addConditionImpl(Condition condition) {
        conditions.add(condition);
        userParameters.addAll(condition.getUserParameters());
    }

    public List<Object> getUserParameters() {
        return userParameters;
    }

    public abstract T execute() throws SQLException;
}
