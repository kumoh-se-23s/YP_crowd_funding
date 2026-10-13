package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;

import java.util.List;

public final class ScalarSubqueryColumnSelector extends ColumnSelector {
    SQLSelect query;

    public ScalarSubqueryColumnSelector(SQLSelect query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return query.toString();
    }

    @Override
    public List<Object> getUserParameters() {
        return query.getUserParameters();
    }
}
