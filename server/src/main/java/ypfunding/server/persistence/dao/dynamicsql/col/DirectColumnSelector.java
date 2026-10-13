package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.ColumnsEnum;

import java.util.List;

public final class DirectColumnSelector extends ColumnSelector {
    ColumnsEnum column;

    public DirectColumnSelector(ColumnsEnum column) {
        this.column = column;
    }

    @Override
    public String toString() {
        return column.toString();
    }

    @Override
    public List<Object> getUserParameters() {
        return List.of();
    }
}
