package ypfunding.server.persistence.dao.dynamicsql.table;

import java.util.List;

public final class DirectReferenceTable extends ReferenceTable {
    private final DirectReferenceTables tableName;
    private static final String VAR_PREFIX = "__drt";
    private static int cnt = 0;
    private final int num;

    public DirectReferenceTable(DirectReferenceTables table) {
        this.tableName = table;
        cnt = (cnt + 1) % 1_000_000_007; //arithmetic overflow protection
        this.num = cnt;
    }

    public String getVarName(){
        return VAR_PREFIX + num;
    }

    public DirectReferenceTables getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return tableName.toString() + " as " + getVarName();
    }

    @Override
    public List<Object> getUserParameters() {
        return List.of();
    }
}
