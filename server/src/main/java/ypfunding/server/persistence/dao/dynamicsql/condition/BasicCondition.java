package ypfunding.server.persistence.dao.dynamicsql.condition;

import ypfunding.server.persistence.dao.dynamicsql.op.ConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.token.*;

import java.util.List;
import java.util.stream.Stream;

public final class BasicCondition extends Condition {
    private final ValueToken lhs;
    private final ConditionOperator operator;
    private final ValueToken rhs;

    public BasicCondition(ValueToken lhs, ConditionOperator operator, ValueToken rhs) {
        super(Stream.of(lhs.getUserParameters(), rhs.getUserParameters()).flatMap(List::stream).toList());
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    private boolean noParenthesesRequired(ValueToken token){
        return token instanceof UserValueToken || token instanceof ColumnValueToken || token instanceof BuiltinFunctionToken
               || token instanceof WildcardValueToken || token instanceof TableRefColumnToken;
    }

    @Override
    public String toString() {
        String lhsStr = noParenthesesRequired(lhs) ? lhs.toString() : ("(" + lhs + ")");
        String rhsStr = noParenthesesRequired(rhs) ? rhs.toString() : ("(" + rhs + ")");
        return lhsStr + operator + rhsStr;
    }
}
