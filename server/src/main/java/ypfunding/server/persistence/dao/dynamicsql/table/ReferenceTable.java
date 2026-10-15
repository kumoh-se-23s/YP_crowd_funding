package ypfunding.server.persistence.dao.dynamicsql.table;

import java.util.List;

public abstract sealed class ReferenceTable permits DirectReferenceTable, InlineViewReferenceTable, JoinedTable, PredefinedReferenceTable {

    private static int cnt = 0;
    protected final int num;

    protected ReferenceTable() {
        cnt = (cnt + 1) % 1_000_000_007; //arithmetic overflow protection
        this.num = cnt;
    }

    protected abstract String getVarPrefix();

    public String getVarName(){
        return getVarPrefix() + num;
    }


    public abstract List<Object> getUserParameters();
}
