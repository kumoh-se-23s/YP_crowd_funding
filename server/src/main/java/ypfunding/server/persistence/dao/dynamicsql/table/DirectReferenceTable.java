package ypfunding.server.persistence.dao.dynamicsql.table;

import java.util.List;

public final class DirectReferenceTable extends ReferenceTable {
    private final String tableName;

    public DirectReferenceTable(String table) {
        this.tableName = table;
    }

    @Override
    public String toString() {
        return tableName;
    }

    @Override
    public List<Object> getUserParameters() {
        return List.of();
    }
}
