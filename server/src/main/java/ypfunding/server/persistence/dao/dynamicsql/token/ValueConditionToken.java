package ypfunding.server.persistence.dao.dynamicsql.token;

import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.op.ConditionOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValueConditionToken extends ValueToken {
    private final Condition condition;

    public ValueConditionToken(Condition condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return condition.toString();
    }

    @Override
    public List<Object> getUserParameters() {
        return Collections.unmodifiableList(condition.getUserParameters());
    }
}
