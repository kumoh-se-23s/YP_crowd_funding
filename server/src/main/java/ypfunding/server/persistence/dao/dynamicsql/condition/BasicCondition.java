package ypfunding.server.persistence.dao.dynamicsql.condition;

import ypfunding.server.persistence.dao.dynamicsql.op.ConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;

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

    @Override
    public String toString() {
        return "(" + lhs + ")" + operator + "(" + rhs + ")";
    }
}
