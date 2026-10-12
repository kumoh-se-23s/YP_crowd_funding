package ypfunding.server.persistence.dao.dynamicsql;

import java.util.ArrayList;
import java.util.List;

public final class RawValueToken extends ValueToken {
    private final Number value;

    public RawValueToken(Number value) {
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
