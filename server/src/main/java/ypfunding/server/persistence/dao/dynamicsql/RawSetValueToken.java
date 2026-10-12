package ypfunding.server.persistence.dao.dynamicsql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class RawSetValueToken extends SetValueToken {
    private final String[] set;

    public RawSetValueToken(String... set) {
        this.set = set;
    }

    @Override
    public String toString() {

        return "(" + Arrays.stream(set).map(_ -> "?").collect(Collectors.joining(",")) + ")";
    }

    @Override
    public List<Object> getUserParameters() {
        // containing user parameters.
        return new ArrayList<>(Arrays.asList(set));
    }
}
