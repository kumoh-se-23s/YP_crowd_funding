package ypfunding.server.persistence.dao.dynamicsql.token;

import ypfunding.server.persistence.dao.dynamicsql.col.ScalarSubqueryColumnSelector;

import java.util.List;

public final class ScalarSubqueryReferenceToken extends ValueToken{

    private final ScalarSubqueryColumnSelector columnSelector;
    public ScalarSubqueryReferenceToken(ScalarSubqueryColumnSelector columnSelector){
        this.columnSelector = columnSelector;
    }

    @Override
    public String toString() {
        return columnSelector.getResultColumnName();
    }

    @Override
    public List<Object> getUserParameters() {
        return List.of();
    }
}
