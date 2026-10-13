package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;

import java.util.List;

public final class ScalarSubqueryColumnSelector extends ColumnSelector {
    private final SQLSelect query;
    private final String resultColumnName;

    public ScalarSubqueryColumnSelector(SQLSelect query){
        this(query, "");
    }
    public ScalarSubqueryColumnSelector(SQLSelect query, String resultColumnName) {
        this.query = query;
        this.resultColumnName = resultColumnName;
    }


    public String getResultColumnName() {
        return resultColumnName;
    }

    @Override
    public String toString() {
        if(resultColumnName.isBlank()){
            return "(" + query.toString() + ")";
        }
        return "(" + query.toString() + ") as " + resultColumnName;
    }

    @Override
    public List<Object> getUserParameters() {
        return query.getUserParameters();
    }
}
