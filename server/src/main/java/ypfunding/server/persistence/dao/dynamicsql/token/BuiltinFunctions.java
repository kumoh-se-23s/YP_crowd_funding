package ypfunding.server.persistence.dao.dynamicsql.token;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public enum BuiltinFunctions {
    COUNT(1),
    ROUND(2),
    ABS(1),
    UPPER(1),
    LOWER(1),
    SUM(1);

    private final int parameterCounts;

    BuiltinFunctions(int parameterCounts) {
        this.parameterCounts = parameterCounts;
    }
    private ValueToken apply(ValueToken... params){
        if(params.length != parameterCounts) throw new IllegalArgumentException("parameter count of '" + this + "'does not match");
        return new CombinedValueToken(this + "(" + Arrays.stream(params).map(Object::toString).collect(Collectors.joining(",")) + ")", ValueToken.combineUserParameters(params));
    }

    public static ValueToken makeFunction(BuiltinFunctions token, ValueToken... params) {
        return token.apply(params);
    }

}
