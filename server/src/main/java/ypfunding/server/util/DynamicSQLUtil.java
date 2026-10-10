package ypfunding.server.util;

import ypfunding.server.persistence.dao.ColumnsEnum;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DynamicSQLUtil {

    private DynamicSQLUtil(){

    }
    /**
     *
     * 1st {} is replaced to a, b, c, ... <br/>
     * 2nd {} is replaced to ?, ?, ?, ... <br/>
     * example <br/>
     * <code>String sql = insert into table ({}) values ({})<br/>
     * sql = replace(sql, {a, b, c})<br/>
     * // sql = "insert into table (a, b, c) values (?, ?, ?)"<br/>
     * </code><br/>
     * the contents of two braces must be empty.<br/>
     * if the braces count in the given sql is not equal to 2, the behavior is undefined and results an exception. <br/>
     */
    public static String generate(String sql, ColumnsEnum... keys){
        sql = sql.replaceFirst("\\Q{}\\E", Arrays.stream(keys).map(Object::toString).collect(Collectors.joining(",")));
        sql = sql.replaceFirst("\\Q{}\\E", Arrays.stream(keys).map(_ -> "?").collect(Collectors.joining(",")));
        return sql;
    }

}
