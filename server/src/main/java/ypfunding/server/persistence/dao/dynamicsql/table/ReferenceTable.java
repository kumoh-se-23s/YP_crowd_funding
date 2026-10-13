package ypfunding.server.persistence.dao.dynamicsql.table;

import java.util.List;

public abstract sealed class ReferenceTable permits DirectReferenceTable, InlineViewReferenceTable, JoinedTable {

    protected ReferenceTable() {

    }

    public abstract List<Object> getUserParameters();
}
