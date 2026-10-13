package ypfunding.server.persistence.dao.dynamicsql.op;

public enum SetConditionOperator implements ConditionOperator {
    NI("NOT IN"),
    IN("IN");

    private final String str;

    SetConditionOperator(String s) {
        str = s;
    }

    @Override
    public String toString() {
        return str;
    }
}
