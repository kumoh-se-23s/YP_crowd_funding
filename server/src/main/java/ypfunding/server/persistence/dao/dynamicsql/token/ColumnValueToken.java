package ypfunding.server.persistence.dao.dynamicsql.token;

import ypfunding.server.persistence.dao.ColumnsEnum;

import java.util.List;

public final class ColumnValueToken extends ValueToken{

    private final ColumnsEnum column;

    public ColumnValueToken(ColumnsEnum column) {
        this.column = column;
    }

    @Override
    public String toString() {
        return column.toString();
    }

    @Override
    public List<Object> getUserParameters(){
        return List.of();
    }
}
