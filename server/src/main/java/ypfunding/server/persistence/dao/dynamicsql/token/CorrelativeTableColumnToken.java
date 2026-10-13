package ypfunding.server.persistence.dao.dynamicsql.token;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;

import java.util.List;

public final class CorrelativeTableColumnToken extends ValueToken{

    private final DirectReferenceTable table;
    private final ColumnsEnum column;
    public CorrelativeTableColumnToken(DirectReferenceTable table, ColumnsEnum column){
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
