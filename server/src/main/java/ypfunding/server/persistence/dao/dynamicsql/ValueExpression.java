package ypfunding.server.persistence.dao.dynamicsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValueExpression extends ValueToken {
    private final ValueToken lhs;
    private final ArithmeticOperator operator;
    private final ValueToken rhs;

    public ValueExpression(ValueToken lhs, ArithmeticOperator operator, ValueToken rhs) {
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    @Override
    public String toString() {
        return lhs.toString() + operator + rhs;
    }

    @Override
    public List<Object> getUserParameters() {
        List<Object> userParams = new ArrayList<>();
        userParams.addAll(lhs.getUserParameters());
        userParams.addAll(rhs.getUserParameters());
        return Collections.unmodifiableList(userParams);
    }
}
