
import kr.yp_crowdfunding.Application;

import java.sql.SQLException;

void main() {
    try (Application application = new Application()) {
        application.start();
    } catch (SQLException e) {
        System.out.println("SQL Error : " + e);
    }
}

