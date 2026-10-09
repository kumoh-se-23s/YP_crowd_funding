package ypfunding.server.persistence;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlWork<T> {
        T run(Connection conn) throws SQLException;
}
