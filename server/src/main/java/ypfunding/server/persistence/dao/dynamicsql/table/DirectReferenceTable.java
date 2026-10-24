package ypfunding.server.persistence.dao.dynamicsql.table;

import java.util.List;

public final class DirectReferenceTable extends VarNameReferenceTable {
    private final DirectReferenceTables tableName;

    public DirectReferenceTable(DirectReferenceTables table) {
        this.tableName = table;
    }

    @Override
    public String getVarPrefix() {
        return "__drt";
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
