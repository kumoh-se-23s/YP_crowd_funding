package ypfunding.server.persistence.dao.dynamicsql.token;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.table.VarNameReferenceTable;

import java.util.List;

public final class TableRefColumnToken extends ValueToken{

    private final VarNameReferenceTable table;
    private final ColumnsEnum column;
    public TableRefColumnToken(VarNameReferenceTable table, ColumnsEnum column){
        this.table = table;
        this.column = column;
    }

    @Override
    public String toString() {
        return table.getVarName() + "." + column;
    }

    @Override
    public List<Object> getUserParameters() {
        return List.of();
    }
}
