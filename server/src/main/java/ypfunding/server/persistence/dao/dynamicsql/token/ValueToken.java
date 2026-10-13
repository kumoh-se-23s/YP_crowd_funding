package ypfunding.server.persistence.dao.dynamicsql.token;

import java.util.Arrays;
import java.util.List;

public abstract sealed class ValueToken permits BuiltinFunctionToken, ColumnValueToken, CombinedValueToken, CorrelatedTableValueToken, ScalarSubqueryReferenceToken, SetValueToken, SubqueryToken, CorrelativeTableColumnToken, UncorrelatedTableValueToken, UserValueToken, ValueExpressionToken, WildcardValueToken {

    protected ValueToken() {
    }

    public static List<Object> combineUserParameters(ValueToken... tokens) {
        return Arrays.stream(tokens).flatMap(e -> e.getUserParameters().stream()).toList();
    }

    public abstract List<Object> getUserParameters();
}
