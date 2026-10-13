package ypfunding.server.persistence.dao.dynamicsql.condition;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.op.ConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;

import java.util.ArrayList;

public final class BasicCondition extends Condition {
    private final ColumnsEnum column;
    private final ConditionOperator operator;
    private final ValueToken targetValue;

    BasicCondition(ColumnsEnum column, ConditionOperator operator, ValueToken targetValue) {
        super(new ArrayList<>());
        this.column = column;
        this.operator = operator;
        this.targetValue = targetValue;
    }

    @Override
    public String toString() {
        return column.toString() + operator + targetValue;
    }
}
