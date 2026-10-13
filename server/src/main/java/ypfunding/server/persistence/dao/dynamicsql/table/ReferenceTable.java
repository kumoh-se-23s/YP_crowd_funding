package ypfunding.server.persistence.dao.dynamicsql.table;

import java.util.List;

public abstract sealed class ReferenceTable permits
        DirectReferenceTable,
        InlineViewReferenceTable {

    protected ReferenceTable() {

    }

    public abstract List<Object> getUserParameters();
}
