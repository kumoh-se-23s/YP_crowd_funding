package ypfunding.server.persistence.dao.dynamicsql.op;

public enum LogicalConditionOperator implements ConditionOperator {
    AND("AND"),
    OR("OR"),
    NOT("NOT")
    ;
    private final String str;

    LogicalConditionOperator(String s) {
        str = s;
    }

    @Override
    public String toString() {
        return str;
    }

}
