package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;


public final class SimpleOrderBySelector extends OrderBySelector{
    private final ColumnsEnum target;

    public SimpleOrderBySelector(ColumnsEnum target, boolean ascending) {
        super(ascending);
        this.target = target;
    }

    @Override
    public ValueToken getTarget() {
        return new ColumnValueToken(target);
    }

}
