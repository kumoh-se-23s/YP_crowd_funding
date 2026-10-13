package ypfunding.server.persistence.dao.dynamicsql.token;

import java.util.ArrayList;
import java.util.List;

public final class UserValueToken extends ValueToken {
    private final Object value;

    public UserValueToken(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "?";
    }

    @Override
    public List<Object> getUserParameters() {
        // may contain user parameters
        List<Object> r = new ArrayList<>();

        if (value == null) {
            r.add("null");
        } else if (value instanceof Number) {
            r.add(value.toString());
        } else {
            r.add("'" + value.toString().replace("'", "''") + "'");
        }

        return r;
    }
}
