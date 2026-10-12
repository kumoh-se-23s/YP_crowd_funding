package ypfunding.server.persistence.dao.dynamicsql;

import ypfunding.server.persistence.dao.ColumnsEnum;

import java.util.List;

public final class CorrelatedTableValueToken extends ValueToken {
    private final String varName;
    private final ColumnsEnum column;

    public CorrelatedTableValueToken(String varName, ColumnsEnum column) {
        this.varName = varName;
        this.column = column;
    }

    @Override
    public String toString() {
        return varName + "." + column;
    }

    @Override
    public List<Object> getUserParameters() {
        //direct reference of column name. no user parameters
        return List.of();
    }
}
