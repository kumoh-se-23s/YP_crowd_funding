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
        r.add(value);
        return r;
    }
}
