package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueExpressionToken;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;

import java.util.List;

public final class ColumnFromTokenSelector extends ColumnSelector{
    private final ColumnsEnum name;
    private final ValueToken token;

    public ColumnFromTokenSelector(ValueToken token) {
        this(null, token);
    }

    public ColumnFromTokenSelector(ColumnsEnum name, ValueToken token) {
        this.name = name;
        this.token = token;
    }

    @Override
    public List<Object> getUserParameters() {
        return token.getUserParameters();
    }

    @Override
    public String toString() {
        return name == null ? token.toString() : "(" + token + ") as " + name;
    }
}
