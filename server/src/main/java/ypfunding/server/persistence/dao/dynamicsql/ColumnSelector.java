package ypfunding.server.persistence.dao.dynamicsql;

import java.util.List;
import java.util.stream.Collectors;

public abstract sealed class ColumnSelector permits DirectColumnSelector, ScalarSubqueryColumnSelector {

    public abstract List<Object> getUserParameters();

    public static String buildColumnParentheses(List<ColumnSelector> selectors) {
        return "(" + selectors.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
    }

    public static String buildColumnParenthesesWithWildcard(List<ColumnSelector> selectors) {
        return buildColumnParentheses(selectors) +
               " values (" +
               selectors.stream()
                       .map(_ -> "?")
                       .collect(Collectors.joining(",")) + ")";
    }
}
