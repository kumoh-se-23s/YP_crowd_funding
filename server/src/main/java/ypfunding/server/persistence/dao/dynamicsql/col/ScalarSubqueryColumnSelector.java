package ypfunding.server.persistence.dao.dynamicsql.col;

import ypfunding.server.persistence.dao.ColumnsEnum;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;

import java.util.List;

public final class ScalarSubqueryColumnSelector extends ColumnSelector {

    private final ColumnsEnum resultColumnName;
    private final SQLSelect query;

    public ScalarSubqueryColumnSelector(SQLSelect query){
        this(null, query);
    }
    public ScalarSubqueryColumnSelector(ColumnsEnum resultColumnName, SQLSelect query) {
        this.resultColumnName = resultColumnName;
        this.query = query;
    }


    public String getResultColumnName() {
        return resultColumnName.toString();
    }

    @Override
    public String toString() {
        if(resultColumnName == null){
            return "(" + query.toString() + ")";
        }
        return "(" + query.toString() + ") as " + resultColumnName;
    }

    @Override
    public List<Object> getUserParameters() {
        return query.getUserParameters();
    }
}
