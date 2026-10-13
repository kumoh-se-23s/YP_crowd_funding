package ypfunding.server.persistence.dao.dynamicsql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccumulativePreparedStatement implements AutoCloseable {
    private final PreparedStatement statement;
    private final List<Object> userParameters;

    public AccumulativePreparedStatement(Connection conn, String sql) throws SQLException {
        this.statement = conn.prepareStatement(sql);
        this.userParameters = new ArrayList<>();
    }

    public void addParams(List<Object> params) {
        this.userParameters.addAll(params);
    }

    public void addParam(Object param) {
        userParameters.add(param);
    }

    public PreparedStatement applyParam() throws SQLException {
        for (int i = 0; i < userParameters.size(); ++i) {
            statement.setObject(i + 1, userParameters.get(i));
        }
        return statement;
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }
}
