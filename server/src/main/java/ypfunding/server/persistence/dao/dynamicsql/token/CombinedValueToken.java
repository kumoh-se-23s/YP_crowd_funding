package ypfunding.server.persistence.dao.dynamicsql.token;

import java.util.Collections;
import java.util.List;

public final class CombinedValueToken extends ValueToken {
    private final String condition;
    private final List<Object> parameters;

    CombinedValueToken(String condition, List<Object> parameters) {
        this.condition = condition;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return condition;
    }

    @Override
    public List<Object> getUserParameters() {
        return Collections.unmodifiableList(parameters);
    }
}
