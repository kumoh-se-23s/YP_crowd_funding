package ypfunding.server.persistence.dao.dynamicsql.table;

import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;

import java.util.List;

public final class InlineViewReferenceTable extends ReferenceTable {
    SQLSelect query;

    public InlineViewReferenceTable(SQLSelect query) {
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
