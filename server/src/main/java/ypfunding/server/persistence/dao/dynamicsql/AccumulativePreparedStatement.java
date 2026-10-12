package ypfunding.server.persistence.dao.dynamicsql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class AccumulativePreparedStatement implements AutoCloseable {
    private final PreparedStatement statement;
    private final List<Object> params;

    public AccumulativePreparedStatement(Connection conn, String sql) throws SQLException {
        this.statement = conn.prepareStatement(sql);
        this.params = new ArrayList<>();
    }

    public void addParams(List<Object> params) {
        this.params.addAll(params);
    }

    public void addParam(Object param) {
        params.add(param);
    }

    public PreparedStatement applyParam() throws SQLException {
        for (int i = 0; i < params.size(); ++i) {
            statement.setObject(i + 1, params.get(i));
        }
        return statement;
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }
}
