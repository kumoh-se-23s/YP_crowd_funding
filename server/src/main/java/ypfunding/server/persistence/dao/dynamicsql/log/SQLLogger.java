package ypfunding.server.persistence.dao.dynamicsql.log;

import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLBuilder;
import ypfunding.server.persistence.dao.dynamicsql.table.PredefinedReferenceTable;

import java.util.List;
import java.util.stream.Collectors;

public class SQLLogger {

    private SQLLogger() {

    }


    public static void log(Severity severity, String str) {
        //TODO set out to other

        System.out.printf("%s | %s | %s \n", DateTimeUtil.now(), severity, str);
    }
    public static String getQuery(SQLBuilder<?> sqlBuilder){
        return sqlBuilder.toString();
    }

    public static void logQuery(SQLBuilder<?> sqlBuilder) {
        String params = sqlBuilder.getUserParameters().stream().map(Object::toString).collect(Collectors.joining(", "));
//        System.out.printf("SQL Query Executed - %s\n\tparameters : %s\n", getQuery(sqlBuilder), params.isBlank() ? "[ NO PARAMETERS ]" : params);
    }
}
