package ypfunding.server.persistence.dao.dynamicsql.token;

import ypfunding.server.persistence.dao.ColumnsEnum;

import java.util.List;

public final class UncorrelatedTableValueToken extends ValueToken {
    private final ColumnsEnum column;

    public UncorrelatedTableValueToken(ColumnsEnum column) {
        this.column = column;
    }

    @Override
    public String toString() {
        return column.toString();
    }

    @Override
    public List<Object> getUserParameters() {
        //direct reference of column name. no user parameters
        return List.of();
    }
}
