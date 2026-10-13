package ypfunding.server.persistence.dao.dynamicsql.log;

import ypfunding.server.persistence.dao.dynamicsql.builder.SQLBuilder;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class SQLLogger {

    private SQLLogger() {

    }


    public static void log(Severity severity, String str) {
        //TODO set out to other

        System.out.printf("%s | %s | %s \n", LocalDateTime.now(), severity, str);
    }


    public static void logQuery(SQLBuilder sqlBuilder) {
        System.out.printf("SQL Query Executed - %s\n\tparameters : %s\n", sqlBuilder.toString(), String.join(", ", sqlBuilder.getUserParameters().stream().map(Object::toString).toArray(String[]::new)));
    }
}
