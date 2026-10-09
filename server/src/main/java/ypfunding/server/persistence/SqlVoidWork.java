package ypfunding.server.persistence;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlVoidWork {
    void run(Connection conn) throws SQLException;
}
