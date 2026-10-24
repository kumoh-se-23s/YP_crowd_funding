package ypfunding.server.persistence.dao.dynamicsql.log;

import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLBuilder;

import java.util.stream.Collectors;

public class SQLLogger {

    private SQLLogger() {

    }


    public static void log(Severity severity, String str) {
        //TODO set out to other

        System.out.printf("%s | %s | %s \n", DateTimeUtil.now(), severity, str);
    }


    public static void logQuery(SQLBuilder<?> sqlBuilder) {
        String params = sqlBuilder.getUserParameters().stream().map(Object::toString).collect(Collectors.joining(", "));
        System.out.printf("SQL Query Executed - %s\n\tparameters : %s\n", sqlBuilder.toString(), params.isBlank() ? "[ NO PARAMETERS ]" : params);
    }
}
