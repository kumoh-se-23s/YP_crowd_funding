package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;


public final class GenericOrderBySelector extends OrderBySelector{
    private final ValueToken target;

    public GenericOrderBySelector(ValueToken target, boolean ascending) {
        super(ascending);
        this.target = target;
    }

    @Override
    public ValueToken getTarget() {
        return target;
    }

}
