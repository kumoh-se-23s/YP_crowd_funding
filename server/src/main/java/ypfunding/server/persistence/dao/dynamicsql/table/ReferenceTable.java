package ypfunding.server.persistence.dao.dynamicsql.table;

import java.util.List;

public abstract sealed class ReferenceTable permits VarNameReferenceTable, JoinedTable {


    public abstract List<Object> getUserParameters();
}
