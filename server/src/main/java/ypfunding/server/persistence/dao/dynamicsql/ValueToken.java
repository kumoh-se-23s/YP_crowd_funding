package ypfunding.server.persistence.dao.dynamicsql;

import java.util.List;

public abstract sealed class ValueToken permits CorrelatedTableValueToken, RawValueToken, SetValueToken, SubqueryToken, UncorrelatedTableValueToken, ValueExpression {
    protected ValueToken() {
    }

    public abstract List<Object> getUserParameters();
}
