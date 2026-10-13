package ypfunding.server.persistence.dao.dynamicsql.table;

import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class PredefinedReferenceTable extends ReferenceTable {
    private final SQLSelect query;

    public PredefinedReferenceTable(SQLSelect query){
        this.query = query;

    }

    @Override
    public String getVarPrefix() {
        return "__prt";
    }

    @Override
    public String toString() {
        return getVarName() + " as (" + query + ")";
    }

    @Override
    public List<Object> getUserParameters() {
        return query.getUserParameters();
    }

    public static String makeQuery(PredefinedReferenceTable... tables){
        return tables.length == 0 ? "" : "with " + Arrays.stream(tables).map(Object::toString).collect(Collectors.joining(","));
    }
}
