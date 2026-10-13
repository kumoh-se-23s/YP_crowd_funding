package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.ColumnsEnum;

import java.util.List;
import java.util.stream.Collectors;

public abstract sealed class ColumnSelector permits
        DirectColumnSelector,
        ScalarSubqueryColumnSelector {

    public abstract List<Object> getUserParameters();

    public static String buildSelectColumnParentheses(List<ColumnSelector> selectors) {
        return selectors.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    public static String buildColumnParenthesesWithValues(List<ColumnsEnum> selectors) {
        return "(" + selectors.stream().map(Object::toString).collect(Collectors.joining(", ")) +
               ") values (" +
               selectors.stream()
                       .map(_ -> "?")
                       .collect(Collectors.joining(",")) + ")";
    }
}
