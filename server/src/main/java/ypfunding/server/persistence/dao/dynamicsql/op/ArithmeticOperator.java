package ypfunding.server.persistence.dao.dynamicsql.op;

public enum ArithmeticOperator {
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/");

    private final String str;

    ArithmeticOperator(String s) {
        str = s;
    }


    @Override
    public String toString() {
        return str;
    }
}
