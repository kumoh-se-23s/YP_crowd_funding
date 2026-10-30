package ypfunding.server.persistence.dao.dynamicsql.col;

import lombok.NonNull;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;

import java.util.List;

public abstract sealed class OrderBySelector permits GenericOrderBySelector, SimpleOrderBySelector {
    private final boolean ascending;

    protected OrderBySelector(boolean ascending) {
        this.ascending = ascending;
    }

    public abstract ValueToken getTarget();

    @Override
    @NonNull
    public String toString() {
        return getTarget() + (ascending ? " asc" : " desc");
    }

    public List<Object> getUserParameters() {
        return getTarget().getUserParameters();
    }

}
