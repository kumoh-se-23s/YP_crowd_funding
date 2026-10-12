package ypfunding.server.persistence.dao.dynamicsql;

import java.util.List;

public final class CombinedCondition extends Condition {
    String condition;

    CombinedCondition(String condition, List<Object> parameters) {
        super(parameters);
        this.condition = condition;
    }


    @Override
    public String toString() {
        return condition;
    }
}
