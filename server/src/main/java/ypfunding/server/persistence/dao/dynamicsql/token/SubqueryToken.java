package ypfunding.server.persistence.dao.dynamicsql.token;

import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;

import java.util.ArrayList;
import java.util.List;

public final class SubqueryToken extends ValueToken {
    private final SQLSelect selectQuery;

    public SubqueryToken(SQLSelect selectQuery) {
        this.selectQuery = selectQuery;
    }

    @Override
    public String toString() {
        return "(" + selectQuery + ")";
    }

    @Override
    public List<Object> getUserParameters() {
        return new ArrayList<>(selectQuery.getUserParameters());
    }
}
