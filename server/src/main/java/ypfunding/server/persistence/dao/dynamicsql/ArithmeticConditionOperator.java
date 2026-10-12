package ypfunding.server.persistence.dao.dynamicsql;

public enum ArithmeticConditionOperator implements ConditionOperator {
    LE("<="),
    EQ("="),
    GE(">="),
    GT(">"),
    LT("<"),
    NE("!=");
    private final String str;

    ArithmeticConditionOperator(String s) {
        str = s;
    }

    @Override
    public String toString() {
        return str;
    }

}
