package ypfunding.server.persistence.dao.dynamicsql.table;

import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class PredefinedReferenceTable extends VarNameReferenceTable {
    private final SQLSelect query;

    public PredefinedReferenceTable(SQLSelect query){
        this.query = query;

    }

    @Override
    public String getVarPrefix() {
        return "__prt";
    }

    /** 참조할 때 (FROM / JOIN 자리): 이름만 */
    @Override
    public String toString() {
        return getVarName();
    }

    /** 정의할 때 (WITH 절): 이름 as (쿼리) */
    public String toDefinition() {
        return getVarName() + " as (" + query + ")";
    }

    /** 참조할 때는 SQL에 ?가 없으므로 값도 없음 */
    @Override
    public List<Object> getUserParameters() {
        return List.of();
    }

    /** 정의(WITH 절)에 들어가는 ?의 값 */
    public List<Object> getDefinitionParameters() {
        return query.getUserParameters();
    }
    public static String makeQuery(PredefinedReferenceTable... tables){
        return tables.length == 0 ? "" : "with " + Arrays.stream(tables).map(PredefinedReferenceTable::toDefinition).collect(Collectors.joining(","));
    }
}
