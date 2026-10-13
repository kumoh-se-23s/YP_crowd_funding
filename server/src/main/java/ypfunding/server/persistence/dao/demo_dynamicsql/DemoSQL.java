package ypfunding.server.persistence.dao.demo_dynamicsql;

import java.sql.Connection;
import java.sql.SQLException;

public interface DemoSQL {

    void execute(Connection conn) throws SQLException;
}
